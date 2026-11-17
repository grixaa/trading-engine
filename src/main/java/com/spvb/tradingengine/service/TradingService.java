package com.spvb.tradingengine.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.spvb.tradingengine.domain.Instrument;
import com.spvb.tradingengine.domain.MatchPlan;
import com.spvb.tradingengine.domain.Order;
import com.spvb.tradingengine.domain.OrderStatus;
import com.spvb.tradingengine.domain.Side;
import com.spvb.tradingengine.exception.EmptyAmendmentException;
import com.spvb.tradingengine.exception.InvalidPriceException;
import com.spvb.tradingengine.exception.InvalidQuantityException;
import com.spvb.tradingengine.exception.OrderClosedException;
import com.spvb.tradingengine.exception.OrderNotFoundException;
import com.spvb.tradingengine.repository.OrderRepository;
import com.spvb.tradingengine.repository.TradeRepository;
import com.spvb.tradingengine.entity.OrderEntity;
import com.spvb.tradingengine.entity.TradeEntity;
import com.spvb.tradingengine.repository.mapper.PersistenceMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class TradingService {
    CatalogService catalogService;
    OrderRepository orderRepository;
    TradeRepository tradeRepository;
    MatchingEngine matchingEngine;
    SettlementService settlementService;

    public Order place(
            final long userId, 
            @NonNull final String seccode,
            @NonNull final Side side,
            @NonNull final BigDecimal price,
            final int quantity
    ) {
        val instrument = catalogService.lockInstrument(seccode);
        validatePrice(instrument, price);
        val plan = plan(userId, seccode, side, price, quantity);
        settlementService.lockParticipants(userId, plan);
        val reservation = instrument.reservation(side, price, quantity);
        settlementService.changeReservation(userId, instrument.reservedAsset(side), reservation);
        val priority = orderRepository.nextPriority();
        val now = OffsetDateTime.now(ZoneOffset.UTC);
        val entity = OrderEntity.builder()
                .userId(userId)
                .seccode(seccode)
                .side(side)
                .price(price)
                .quantity(quantity)
                .status(OrderStatus.OPEN)
                .priority(priority)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return execute(instrument, PersistenceMapper.toOrder(orderRepository.save(entity)), plan);
    }

    public Order amend(
            final long userId,
            final long orderId,
            @NonNull final BigDecimal price,
            @NonNull final Integer remaining
    ) {
        val instrument = lockOrderInstrument(userId, orderId);
        val entity = ownedOrder(userId, orderId);
        val order = PersistenceMapper.toOrder(entity);
        requireActive(order);
        if (price == null && remaining == null) {
            throw new EmptyAmendmentException("Укажите новую цену или открытый остаток заявки");
        }
        val newPrice = price == null ? order.price() : price;
        val newRemaining = remaining == null ? order.remaining() : remaining.intValue();
        validatePrice(instrument, newPrice);
        if (newRemaining > Integer.MAX_VALUE - order.filled()) {
            throw new InvalidQuantityException("Общее количество лотов не должно превышать %d".formatted(Integer.MAX_VALUE));
        }

        val priceChanged = newPrice.compareTo(order.price()) != 0;
        if (!priceChanged && newRemaining == order.remaining()) {
            return order;
        }
        val resetPriority = priceChanged || newRemaining > order.remaining();
        val plan = resetPriority
                ? plan(userId, order.seccode(), order.side(), newPrice, newRemaining)
                : new MatchPlan(List.of(), false);
        settlementService.lockParticipants(userId, plan);
        val previousReserve = instrument.reservation(order.side(), order.price(), order.remaining());
        val newReserve = instrument.reservation(order.side(), newPrice, newRemaining);
        settlementService.changeReservation(userId, instrument.reservedAsset(order.side()), newReserve.subtract(previousReserve));
        val priority = resetPriority ? orderRepository.nextPriority() : order.priority();
        entity.setPrice(newPrice);
        entity.setQuantity(order.filled() + newRemaining);
        entity.setPriority(priority);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return execute(instrument, PersistenceMapper.toOrder(entity), plan);
    }

    public Order cancel(final long userId, final long orderId) {
        val instrument = lockOrderInstrument(userId, orderId);
        val order = PersistenceMapper.toOrder(ownedOrder(userId, orderId));
        if (order.status() == OrderStatus.CANCELLED) {
            return order;
        }
        requireActive(order);
        settlementService.lockParticipants(userId, new MatchPlan(List.of(), false));
        return releaseAndCancel(instrument, order);
    }

    private MatchPlan plan(
            final long userId,
            @NonNull final String seccode,
            @NonNull final Side side,
            @NonNull final BigDecimal price,
            final int quantity
    ) {
        try (val crossing = side == Side.BUY ? orderRepository.crossingAsks(seccode, price) : orderRepository.crossingBids(seccode, price)) {
            return matchingEngine.plan(userId, quantity, crossing.map(PersistenceMapper::toOrder).iterator());
        }
    }

    private Order execute(
            @NonNull final Instrument instrument,
            @NonNull final Order incoming,
            @NonNull final MatchPlan plan
    ) {
        int filled = 0;
        for (val execution : plan.executions()) {
            val resting = execution.resting();
            settlementService.settle(instrument, incoming, execution);
            val buy = incoming.side() == Side.BUY ? incoming : resting;
            val sell = incoming.side() == Side.SELL ? incoming : resting;
            tradeRepository.save(TradeEntity.builder()
                    .seccode(buy.seccode())
                    .buyOrderId(buy.id())
                    .sellOrderId(sell.id())
                    .price(resting.price())
                    .quantity(execution.quantity())
                    .executedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build());
            applyFill(orderRepository.getReferenceById(resting.id()), execution.quantity());
            filled += execution.quantity();
        }
        val entity = orderRepository.getReferenceById(incoming.id());
        if (filled > 0) {
            applyFill(entity, filled);
        }
        val result = PersistenceMapper.toOrder(entity);
        return plan.cancelRemainder() ? releaseAndCancel(instrument, result) : result;
    }

    private void applyFill(@NonNull final OrderEntity entity, int executedQuantity) {
        val filled = entity.getFilled() + executedQuantity;
        entity.setFilled(filled);
        entity.setStatus(OrderStatus.forFill(filled, entity.getQuantity()));
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private Order releaseAndCancel(
            @NonNull final Instrument instrument, 
            @NonNull final Order order
    ) {
        val release = instrument.reservation(order.side(), order.price(), order.remaining());
        settlementService.changeReservation(order.userId(), instrument.reservedAsset(order.side()), release.negate());
        val entity = orderRepository.getReferenceById(order.id());
        entity.setStatus(OrderStatus.CANCELLED);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return PersistenceMapper.toOrder(entity);
    }

    private OrderEntity ownedOrder(final long userId, final long id) {
        return orderRepository.findByUserIdAndId(userId, id)
                .orElseThrow(() -> new OrderNotFoundException("Заявка %d не найдена".formatted(id)));
    }

    private Instrument lockOrderInstrument(final long userId, final long id) {
        val seccode = orderRepository.findOwnedInstrument(userId, id)
                .orElseThrow(() -> new OrderNotFoundException("Заявка %d не найдена".formatted(id)));
        return catalogService.lockInstrument(seccode);
    }

    private void requireActive(@NonNull final Order order) {
        if (!order.status().isActive()) {
            throw new OrderClosedException("Заявка %d закрыта".formatted(order.id()));
        }
    }

    private void validatePrice(
            @NonNull final Instrument instrument,
            @NonNull final BigDecimal price
    ) {
        if (price.remainder(instrument.minStep()).signum() != 0) {
            val step = instrument.minStep().stripTrailingZeros().toPlainString();
            throw new InvalidPriceException("Цена должна быть кратна шагу %s".formatted(step));
        }
    }
}

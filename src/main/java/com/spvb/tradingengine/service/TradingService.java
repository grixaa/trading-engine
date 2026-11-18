package com.spvb.tradingengine.service;

import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.spvb.tradingengine.domain.Instrument;
import com.spvb.tradingengine.domain.MatchPlan;
import com.spvb.tradingengine.domain.Order;
import com.spvb.tradingengine.enums.OrderStatus;
import com.spvb.tradingengine.enums.Side;
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

import static com.spvb.tradingengine.repository.mapper.PersistenceMapper.toOrder;
import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class TradingService {
    CatalogService catalogService;
    MatchingEngine matchingEngine;
    OrderRepository orderRepository;
    TradeRepository tradeRepository;
    SettlementService settlementService;

    public Order placeOrder(
            final long userId, 
            @NonNull final String seccode,
            @NonNull final Side side,
            @NonNull final BigDecimal price,
            final int quantity
    ) {
        val instrument = catalogService.lockInstrumentForTrading(seccode);
        validatePriceTick(instrument, price);
        val plan = buildMatchPlan(userId, seccode, side, price, quantity);
        settlementService.lockParticipantAccounts(userId, plan);
        val reservation = instrument.calculateRequiredReserve(side, price, quantity);
        settlementService.adjustAssetReservation(userId, instrument.getReserveAssetCode(side), reservation);
        val priority = orderRepository.nextOrderPriority();
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
        return executeMatchPlan(instrument, toOrder(orderRepository.save(entity)), plan);
    }

    public Order amendOrder(
            final long userId,
            final long orderId,
            @Nullable final BigDecimal price,
            @Nullable final Integer remaining
    ) {
        val instrument = lockInstrumentForOwnedOrder(userId, orderId);
        val entity = getOwnedOrderEntity(userId, orderId);
        val order = toOrder(entity);
        requireActiveOrder(order);
        if (price == null && remaining == null) {
            throw new EmptyAmendmentException("Укажите новую цену или открытый остаток заявки");
        }
        val newPrice = price == null ? order.price() : price;
        val newRemaining = remaining == null ? order.remaining() : remaining.intValue();
        validatePriceTick(instrument, newPrice);
        if (newRemaining > Integer.MAX_VALUE - order.filled()) {
            throw new InvalidQuantityException("Общее количество лотов не должно превышать %d".formatted(Integer.MAX_VALUE));
        }

        val priceChanged = newPrice.compareTo(order.price()) != 0;
        if (!priceChanged && newRemaining == order.remaining()) {
            return order;
        }
        val resetPriority = priceChanged || newRemaining > order.remaining();
        val plan = resetPriority
                ? buildMatchPlan(userId, order.seccode(), order.side(), newPrice, newRemaining)
                : new MatchPlan(List.of(), false);
        settlementService.lockParticipantAccounts(userId, plan);
        val previousReserve = instrument.calculateRequiredReserve(order.side(), order.price(), order.remaining());
        val newReserve = instrument.calculateRequiredReserve(order.side(), newPrice, newRemaining);
        settlementService.adjustAssetReservation(userId, instrument.getReserveAssetCode(order.side()), newReserve.subtract(previousReserve));
        val priority = resetPriority ? orderRepository.nextOrderPriority() : order.priority();
        entity.setPrice(newPrice);
        entity.setQuantity(order.filled() + newRemaining);
        entity.setPriority(priority);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return executeMatchPlan(instrument, toOrder(entity), plan);
    }

    public Order cancelOrder(final long userId, final long orderId) {
        val instrument = lockInstrumentForOwnedOrder(userId, orderId);
        val order = toOrder(getOwnedOrderEntity(userId, orderId));
        if (order.status() == OrderStatus.CANCELLED) {
            return order;
        }
        requireActiveOrder(order);
        settlementService.lockParticipantAccounts(userId, new MatchPlan(List.of(), false));
        return cancelAndReleaseReserve(instrument, order);
    }

    private MatchPlan buildMatchPlan(
            final long userId,
            @NonNull final String seccode,
            @NonNull final Side side,
            @NonNull final BigDecimal price,
            final int quantity
    ) {
        try (val crossing = side == Side.BUY ? orderRepository.streamMatchingSellOrders(seccode, price) : orderRepository.streamMatchingBuyOrders(seccode, price)) {
            return matchingEngine.buildMatchPlan(userId, quantity, crossing.map(PersistenceMapper::toOrder).iterator());
        }
    }

    private Order executeMatchPlan(
            @NonNull final Instrument instrument,
            @NonNull final Order incoming,
            @NonNull final MatchPlan plan
    ) {
        int filled = 0;
        for (val execution : plan.executions()) {
            val resting = execution.resting();
            settlementService.settleExecution(instrument, incoming, execution);
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
            updateOrderAfterFill(orderRepository.getReferenceById(resting.id()), execution.quantity());
            filled += execution.quantity();
        }
        val entity = orderRepository.getReferenceById(incoming.id());
        if (filled > 0) {
            updateOrderAfterFill(entity, filled);
        }
        val result = toOrder(entity);
        return plan.cancelRemainder() ? cancelAndReleaseReserve(instrument, result) : result;
    }

    private void updateOrderAfterFill(@NonNull final OrderEntity entity, final int executedQuantity) {
        val filled = entity.getFilled() + executedQuantity;
        entity.setFilled(filled);
        entity.setStatus(OrderStatus.forFill(filled, entity.getQuantity()));
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private Order cancelAndReleaseReserve(
            @NonNull final Instrument instrument, 
            @NonNull final Order order
    ) {
        val release = instrument.calculateRequiredReserve(order.side(), order.price(), order.remaining());
        settlementService.adjustAssetReservation(order.userId(), instrument.getReserveAssetCode(order.side()), release.negate());
        val entity = orderRepository.getReferenceById(order.id());
        entity.setStatus(OrderStatus.CANCELLED);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return toOrder(entity);
    }

    private OrderEntity getOwnedOrderEntity(final long userId, final long id) {
        return orderRepository.findByUserIdAndId(userId, id)
                .orElseThrow(() -> new OrderNotFoundException("Заявка %d не найдена".formatted(id)));
    }

    private Instrument lockInstrumentForOwnedOrder(final long userId, final long id) {
        val seccode = orderRepository.findInstrumentCodeForUserOrder(userId, id)
                .orElseThrow(() -> new OrderNotFoundException("Заявка %d не найдена".formatted(id)));
        return catalogService.lockInstrumentForTrading(seccode);
    }

    private void requireActiveOrder(@NonNull final Order order) {
        if (!order.status().isActive()) {
            throw new OrderClosedException("Заявка %d закрыта".formatted(order.id()));
        }
    }

    private void validatePriceTick(
            @NonNull final Instrument instrument,
            @NonNull final BigDecimal price
    ) {
        if (price.remainder(instrument.minStep()).signum() != 0) {
            val step = instrument.minStep().stripTrailingZeros().toPlainString();
            throw new InvalidPriceException("Цена должна быть кратна шагу %s".formatted(step));
        }
    }
}

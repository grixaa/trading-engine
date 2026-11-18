package com.spvb.tradingengine.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import com.spvb.tradingengine.entity.OrderEntity;
import com.spvb.tradingengine.entity.TradeEntity;
import com.spvb.tradingengine.enums.Side;
import com.spvb.tradingengine.exception.InvalidPriceException;
import com.spvb.tradingengine.exception.OrderClosedException;
import com.spvb.tradingengine.repository.OrderRepository;
import com.spvb.tradingengine.repository.TradeRepository;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.spvb.tradingengine.TradingFixtures.*;
import static com.spvb.tradingengine.enums.OrderStatus.*;
import static com.spvb.tradingengine.enums.Side.*;
import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = PRIVATE)
class TradingServiceTest {
    @Mock CatalogService catalogService;
    @Spy MatchingEngine matchingEngine = new MatchingEngine();
    @Mock OrderRepository orderRepository;
    @Mock TradeRepository tradeRepository;
    @Mock SettlementService settlementService;
    @InjectMocks TradingService tradingService;

    @Test
    void rejectsPriceThatDoesNotMatchTickBeforeReservingAssets() {
        when(catalogService.lockInstrumentForTrading("DEMO"))
                .thenReturn(stockInstrument());

        val price = new BigDecimal("100.001");

        assertThatThrownBy(() -> tradingService.placeOrder(42, "DEMO", BUY, price, 2))
                .isInstanceOf(InvalidPriceException.class);
        verifyNoInteractions(orderRepository, tradeRepository, settlementService);
    }

    @Test
    void reservesAtLimitAndRecordsPartialTradeAtRestingPrice() {
        val instrument = stockInstrument();
        val limit = new BigDecimal("105.00");
        val resting = openOrder(7, 43, SELL, PRICE, 1);
        val incoming = openOrder(8, 42, BUY, limit, 2);
        when(catalogService.lockInstrumentForTrading("DEMO"))
                .thenReturn(instrument);
        when(orderRepository.streamMatchingSellOrders("DEMO", limit))
                .thenReturn(Stream.of(resting));
        when(orderRepository.nextOrderPriority())
                .thenReturn(8L);
        when(orderRepository.save(any(OrderEntity.class)))
                .thenReturn(incoming);
        when(orderRepository.getReferenceById(7L))
                .thenReturn(resting);
        when(orderRepository.getReferenceById(8L))
                .thenReturn(incoming);

        val result = tradingService.placeOrder(42, "DEMO", BUY, limit, 2);

        assertThat(result.status())
                .isEqualTo(PARTIALLY_FILLED);
        assertThat(result.filled())
                .isEqualTo(1);
        assertThat(result.remaining())
                .isEqualTo(1);
        assertThat(resting.getStatus())
                .isEqualTo(FILLED);

        val trade = ArgumentCaptor.forClass(TradeEntity.class);
        verify(tradeRepository).save(trade.capture());
        assertThat(trade.getValue().getBuyOrderId())
                .isEqualTo(8);
        assertThat(trade.getValue().getSellOrderId())
                .isEqualTo(7);
        assertThat(trade.getValue().getPrice())
                .isEqualByComparingTo("100");
        assertThat(trade.getValue().getQuantity())
                .isEqualTo(1);

        val steps = inOrder(settlementService, orderRepository);
        steps.verify(settlementService)
                .adjustAssetReservation(42, "RUB", new BigDecimal("2100.00"));
        steps.verify(orderRepository)
                .save(any(OrderEntity.class));
    }

    @Test
    void reducesRemainingQuantityWithoutLosingPriorityAndReleasesExcessReserve() {
        val entity = openOrder(7, 42, BUY, PRICE, 2);
        givenOwnedOrder(entity);
        when(orderRepository.getReferenceById(7L))
                .thenReturn(entity);

        val amended = tradingService.amendOrder(42, 7, null, 1);

        assertThat(amended.remaining())
                .isEqualTo(1);
        assertThat(amended.priority())
                .isEqualTo(7);
        verify(settlementService)
                .adjustAssetReservation(42, "RUB", new BigDecimal("-1000.00"));
        verify(orderRepository, never())
                .nextOrderPriority();
        verifyNoInteractions(tradeRepository);
    }

    @Test
    void cancelsOpenOrderOnceAndReleasesReserve() {
        val entity = openOrder(7, 42, BUY, PRICE, 2);
        givenOwnedOrder(entity);
        when(orderRepository.getReferenceById(7L))
                .thenReturn(entity);

        assertThat(tradingService.cancelOrder(42, 7).status())
                .isEqualTo(CANCELLED);
        assertThat(tradingService.cancelOrder(42, 7).remaining())
                .isZero();
        verify(settlementService)
                .adjustAssetReservation(42, "RUB", new BigDecimal("-2000.00"));
        verifyNoInteractions(tradeRepository);
    }

    @Test
    void rejectsCancellationOfFullyFilledOrder() {
        val entity = openOrder(7, 42, BUY, PRICE, 2);
        entity.setFilled(2);
        entity.setStatus(FILLED);
        givenOwnedOrder(entity);

        assertThatThrownBy(() -> tradingService.cancelOrder(42, 7))
                .isInstanceOf(OrderClosedException.class);
        verifyNoInteractions(settlementService, tradeRepository);
    }

    private void givenOwnedOrder(@NonNull final OrderEntity entity) {
        when(orderRepository.findInstrumentCodeForUserOrder(42, 7))
                .thenReturn(Optional.of("DEMO"));
        when(catalogService.lockInstrumentForTrading("DEMO"))
                .thenReturn(stockInstrument());
        when(orderRepository.findByUserIdAndId(42, 7))
                .thenReturn(Optional.of(entity));
    }

    private static OrderEntity openOrder(
            final long id,
            final long userId,
            @NonNull final Side side,
            @NonNull final BigDecimal price,
            final int quantity
    ) {
        return OrderEntity.builder()
                .id(id)
                .userId(userId)
                .seccode("DEMO")
                .side(side)
                .price(price)
                .quantity(quantity)
                .status(OPEN)
                .priority(id)
                .createdAt(TIMESTAMP)
                .updatedAt(TIMESTAMP)
                .build();
    }
}

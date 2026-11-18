package com.spvb.tradingengine.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.spvb.tradingengine.domain.*;
import com.spvb.tradingengine.entity.OrderEntity;
import com.spvb.tradingengine.entity.TradeEntity;
import com.spvb.tradingengine.exception.OrderNotFoundException;
import com.spvb.tradingengine.repository.BookLevelProjection;
import com.spvb.tradingengine.repository.OrderRepository;
import com.spvb.tradingengine.repository.TradeRepository;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;

import static com.spvb.tradingengine.TradingFixtures.*;
import static com.spvb.tradingengine.enums.OrderStatus.OPEN;
import static com.spvb.tradingengine.enums.Side.BUY;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = PRIVATE)
class MarketDataServiceTest {
    @Mock CatalogService catalogService;
    @Mock OrderRepository orderRepository;
    @Mock TradeRepository tradeRepository;
    @Mock BookLevelProjection lowBid;
    @Mock BookLevelProjection highBid;
    @Mock BookLevelProjection lowAsk;
    @Mock BookLevelProjection highAsk;
    @InjectMocks MarketDataService marketDataService;

    @Test
    void rejectsOrderLookupForUnknownOwnerOrOrder() {
        when(orderRepository.findByUserIdAndId(42, 7))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketDataService.getOrderById(42, 7))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void derivesNextOrderCursorFromLastReturnedItem() {
        when(orderRepository.findUserOrdersByFilters(
                42,
                "DEMO",
                BUY,
                OPEN,
                10L,
                Pageable.ofSize(2))
        ).thenReturn(List.of(openOrder(9), openOrder(8)));

        val page = marketDataService.listUserOrders(
                42,
                new OrderFilter("DEMO", BUY, OPEN, 10L, 1)
        );

        assertThat(page.items())
                .extracting(Order::id)
                .containsExactly(9L);
        assertThat(page.nextBeforeId())
                .isEqualTo(9L);
    }

    @Test
    void returnsNoCursorOnLastTradePage() {
        val trade = TradeEntity.builder()
                .id(3L)
                .seccode("DEMO")
                .buyOrderId(7)
                .sellOrderId(8)
                .price(PRICE)
                .quantity(1)
                .executedAt(TIMESTAMP)
                .build();
        when(tradeRepository.findTradesByFilters("DEMO", 7L, null, Pageable.ofSize(3)))
                .thenReturn(List.of(trade));

        val page = marketDataService.listTrades(TradeFilter.builder()
                .seccode("DEMO")
                .orderId(7L)
                .limit(2)
                .build());

        assertThat(page.items())
                .extracting(Trade::id)
                .containsExactly(3L);
        assertThat(page.nextBeforeId())
                .isNull();
    }

    @Test
    void sortsBidsDescendingAndAsksAscending() {
        givenLevel(lowBid, "BUY", 98);
        givenLevel(highBid, "BUY", 99);
        givenLevel(lowAsk, "SELL", 100);
        givenLevel(highAsk, "SELL", 101);
        when(orderRepository.findOrderBookLevels("DEMO", 2))
                .thenReturn(List.of(lowBid, highAsk, highBid, lowAsk));

        val book = marketDataService.getOrderBook("DEMO", 2);

        assertThat(book.bids())
                .extracting(QuoteLevel::price)
                .containsExactly(BigDecimal.valueOf(99), BigDecimal.valueOf(98));
        assertThat(book.asks())
                .extracting(QuoteLevel::price)
                .containsExactly(BigDecimal.valueOf(100), BigDecimal.valueOf(101));
        verify(catalogService).getInstrumentByCode("DEMO");
    }

    private static OrderEntity openOrder(final long id) {
        return OrderEntity.builder()
                .id(id)
                .seccode("DEMO")
                .side(BUY)
                .price(PRICE)
                .quantity(2)
                .status(OPEN)
                .createdAt(TIMESTAMP)
                .updatedAt(TIMESTAMP)
                .build();
    }

    private static void givenLevel(
            @NonNull final BookLevelProjection level,
            @NonNull final String side,
            final int price
    ) {
        when(level.getSide())
                .thenReturn(side);
        when(level.getPrice())
                .thenReturn(BigDecimal.valueOf(price));
        when(level.getQuantity())
                .thenReturn(1L);
    }
}

package com.spvb.tradingengine.api;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import com.spvb.tradingengine.api.dto.AmendOrderRequest;
import com.spvb.tradingengine.api.dto.OrderResponse;
import com.spvb.tradingengine.api.dto.PlaceOrderRequest;
import com.spvb.tradingengine.domain.CursorPage;
import com.spvb.tradingengine.domain.OrderFilter;
import com.spvb.tradingengine.service.MarketDataService;
import com.spvb.tradingengine.service.TradingService;

import static com.spvb.tradingengine.TradingFixtures.PRICE;
import static com.spvb.tradingengine.TradingFixtures.order;
import static com.spvb.tradingengine.enums.OrderStatus.*;
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
class OrderControllerTest {
    @Mock TradingService tradingService;
    @Mock MarketDataService marketDataService;
    @InjectMocks OrderController controller;

    @Test
    void createsOrderAndConvertsLotsToInteger() {
        val placed = order(7, 42, BUY, PARTIALLY_FILLED, 1);
        when(tradingService.placeOrder(42, "DEMO", BUY, PRICE, 2))
                .thenReturn(placed);

        val request = new PlaceOrderRequest("DEMO", BUY, PRICE, BigDecimal.valueOf(2));
        val response = controller.placeOrder(42, request);

        assertThat(response.getStatusCode().value())
                .isEqualTo(201);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("/api/orders/7"));
        assertThat(response.getBody())
                .isEqualTo(OrderResponse.from(placed));
    }

    @Test
    void passesOnlyPriceForPriceOnlyAmendment() {
        val amended = order(7, 42, BUY, OPEN, 0);
        when(tradingService.amendOrder(42, 7, PRICE, null))
                .thenReturn(amended);

        val response = controller.amendOrder(42, 7, new AmendOrderRequest(PRICE, null));

        assertThat(response.getBody())
                .isEqualTo(OrderResponse.from(amended));
    }

    @Test
    void convertsOpenRemainderForQuantityOnlyAmendment() {
        val amended = order(7, 42, BUY, OPEN, 0);
        when(tradingService.amendOrder(42, 7, null, 2))
                .thenReturn(amended);

        val response = controller.amendOrder(
                42,
                7,
                new AmendOrderRequest(null, BigDecimal.valueOf(2))
        );

        assertThat(response.getBody())
                .isEqualTo(OrderResponse.from(amended));
    }

    @Test
    void returnsRequestedOrderAndCancellationResult() {
        val open = order(7, 42, BUY, OPEN, 0);
        val cancelled = order(7, 42, BUY, CANCELLED, 0);
        when(marketDataService.getOrderById(42, 7))
                .thenReturn(open);
        when(tradingService.cancelOrder(42, 7))
                .thenReturn(cancelled);

        assertThat(controller.getOrderById(42, 7).getBody())
                .isEqualTo(OrderResponse.from(open));
        assertThat(controller.cancelOrder(42, 7).getBody())
                .isEqualTo(OrderResponse.from(cancelled));
    }

    @Test
    void forwardsFiltersAndPreservesCursorWhenMappingResponse() {
        val open = order(7, 42, BUY, OPEN, 0);
        val filter = new OrderFilter("DEMO", BUY, OPEN, 8L, 1);
        when(marketDataService.listUserOrders(42, filter))
                .thenReturn(new CursorPage<>(List.of(open), 7L));

        val response = controller.listUserOrders(42, "DEMO", BUY, OPEN, 8L, 1);

        assertThat(response.getStatusCode().value())
                .isEqualTo(200);
        assertThat(response.getBody())
                .isEqualTo(new CursorPage<>(List.of(OrderResponse.from(open)), 7L));
    }
}

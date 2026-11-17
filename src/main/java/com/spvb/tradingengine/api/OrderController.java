package com.spvb.tradingengine.api;

import java.net.URI;

import com.spvb.tradingengine.api.dto.AmendOrderRequest;
import com.spvb.tradingengine.api.dto.OrderResponse;
import com.spvb.tradingengine.api.dto.PlaceOrderRequest;
import com.spvb.tradingengine.domain.CursorPage;
import com.spvb.tradingengine.domain.OrderFilter;
import com.spvb.tradingengine.domain.OrderStatus;
import com.spvb.tradingengine.domain.Side;
import com.spvb.tradingengine.service.MarketDataService;
import com.spvb.tradingengine.service.TradingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.spvb.tradingengine.util.ApiConstants.*;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class OrderController {
    TradingService tradingService;
    MarketDataService marketDataService;

    @PostMapping
    public ResponseEntity<OrderResponse> place(
            @RequestHeader(USER_ID_HEADER)
            @Positive
            final long userId,

            @Valid
            @RequestBody
            final PlaceOrderRequest request
    ) {
        val order = tradingService.place(
                userId,
                request.seccode(),
                request.side(),
                request.price(),
                request.quantity()
                        .intValueExact()
        );

        return ResponseEntity
                .created(URI.create("/api/orders/%s".formatted(order.id())))
                .body(OrderResponse.from(order));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> amend(
            @RequestHeader(USER_ID_HEADER)
            @Positive
            final long userId,

            @PathVariable
            @Positive
            final long id,

            @Valid
            @RequestBody
            final AmendOrderRequest request
    ) {
        val remaining = request.remaining() == null
                ? null
                : Integer.valueOf(request.remaining().intValueExact());

        return ResponseEntity
                .ok(OrderResponse.from(tradingService.amend(userId, id, request.price(), remaining)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponse> cancel(
            @RequestHeader(USER_ID_HEADER)
            @Positive
            final long userId,

            @PathVariable
            @Positive
            final long id
    ) {
        return ResponseEntity
                .ok(OrderResponse.from(tradingService.cancel(userId, id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> order(
            @RequestHeader(USER_ID_HEADER)
            @Positive
            final long userId,

            @PathVariable
            @Positive
            final long id
    ) {
        return ResponseEntity
                .ok(OrderResponse.from(marketDataService.order(userId, id)));
    }

    @GetMapping
    public ResponseEntity<CursorPage<OrderResponse>> orders(
            @RequestHeader(USER_ID_HEADER)
            @Positive
            final long userId,

            @RequestParam(required = false)
            @Pattern(regexp = ASSET_CODE_PATTERN)
            final String seccode,

            @RequestParam(required = false) final Side side,
            @RequestParam(required = false) final OrderStatus status,

            @RequestParam(required = false)
            @Positive
            final Long beforeId,

            @RequestParam(defaultValue = "20")
            @Positive
            @Max(100)
            final int limit
    ) {
        val page = marketDataService.orders(
                userId,
                new OrderFilter(seccode, side, status, beforeId, limit)
        );

        return ResponseEntity
                .ok(new CursorPage<>(
                        page.items().stream()
                                .map(OrderResponse::from)
                                .toList(),
                        page.nextBeforeId()
                ));
    }
}

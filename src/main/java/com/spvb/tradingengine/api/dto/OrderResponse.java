package com.spvb.tradingengine.api.dto;

import lombok.NonNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.spvb.tradingengine.domain.Order;
import com.spvb.tradingengine.domain.OrderStatus;
import com.spvb.tradingengine.domain.Side;
import lombok.Builder;

@Builder
public record OrderResponse(
        long id,
        @NonNull String seccode,
        @NonNull Side side,
        @NonNull BigDecimal price,
        int quantity,
        int filled,
        int remaining,
        @NonNull OrderStatus status,
        @NonNull OffsetDateTime createdAt,
        @NonNull OffsetDateTime updatedAt
) {
    public static OrderResponse from(@NonNull final Order order) {
        return OrderResponse.builder()
                .id(order.id())
                .seccode(order.seccode())
                .side(order.side())
                .price(order.price())
                .quantity(order.quantity())
                .filled(order.filled())
                .remaining(order.remaining())
                .status(order.status())
                .createdAt(order.createdAt())
                .updatedAt(order.updatedAt())
                .build();
    }
}

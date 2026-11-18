package com.spvb.tradingengine.domain;

import com.spvb.tradingengine.enums.OrderStatus;
import com.spvb.tradingengine.enums.Side;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Builder;

@Builder
public record Order(
        long id,
        long userId,
        @NonNull String seccode,
        @NonNull Side side,
        @NonNull BigDecimal price,
        int quantity,
        int filled,
        @NonNull OrderStatus status,
        long priority,
        @NonNull OffsetDateTime createdAt,
        @NonNull OffsetDateTime updatedAt
) {
    public int remaining() {
        return status.isActive()
                ? quantity - filled
                : 0;
    }
}

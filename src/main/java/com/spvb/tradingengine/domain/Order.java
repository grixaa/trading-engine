package com.spvb.tradingengine.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Builder;

@Builder
public record Order(
        long id,
        long userId,
        String seccode,
        Side side,
        BigDecimal price,
        int quantity,
        int filled,
        OrderStatus status,
        long priority,
        OffsetDateTime createdAt,

        OffsetDateTime updatedAt
) {
    public int remaining() {
        return status.isActive()
                ? quantity - filled
                : 0;
    }
}

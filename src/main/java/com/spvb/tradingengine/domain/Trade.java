package com.spvb.tradingengine.domain;

import lombok.NonNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Builder;

@Builder
public record Trade(
        long id,
        @NonNull String seccode,
        long buyOrderId,
        long sellOrderId,
        @NonNull BigDecimal price,
        int quantity,
        @NonNull OffsetDateTime executedAt
) {}

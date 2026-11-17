package com.spvb.tradingengine.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Builder;

@Builder
public record Trade(
        long id,
        String seccode,
        long buyOrderId,
        long sellOrderId,
        BigDecimal price,
        int quantity,
        OffsetDateTime executedAt
) {}

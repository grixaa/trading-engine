package com.spvb.tradingengine.domain;

import org.springframework.lang.Nullable;

import lombok.Builder;

@Builder
public record TradeFilter(
        @Nullable String seccode,
        @Nullable Long orderId,
        @Nullable Long beforeId,
        int limit
) {}

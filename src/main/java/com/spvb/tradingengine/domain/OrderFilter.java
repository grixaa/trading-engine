package com.spvb.tradingengine.domain;

import org.springframework.lang.Nullable;

public record OrderFilter(
        @Nullable String seccode,
        @Nullable Side side,
        @Nullable OrderStatus status,
        @Nullable Long beforeId,
        int limit
) {}

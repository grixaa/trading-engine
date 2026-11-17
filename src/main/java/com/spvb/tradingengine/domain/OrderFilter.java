package com.spvb.tradingengine.domain;

public record OrderFilter(
        String seccode,
        Side side,
        OrderStatus status,
        Long beforeId,
        int limit
) {}

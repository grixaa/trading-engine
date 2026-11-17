package com.spvb.tradingengine.domain;

import lombok.Builder;

@Builder
public record TradeFilter(
        String seccode,
        Long orderId,
        Long beforeId,
        int limit
) {}

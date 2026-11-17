package com.spvb.tradingengine.domain;

import java.math.BigDecimal;

public record BookLevel(
        Side side,
        BigDecimal price,
        long quantity
) {}

package com.spvb.tradingengine.domain;

import lombok.NonNull;

import java.math.BigDecimal;

public record BookLevel(
        @NonNull Side side,
        @NonNull BigDecimal price,
        long quantity
) {}

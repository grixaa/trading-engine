package com.spvb.tradingengine.domain;

import com.spvb.tradingengine.enums.Side;
import lombok.NonNull;

import java.math.BigDecimal;

public record BookLevel(
        @NonNull Side side,
        @NonNull BigDecimal price,
        long quantity
) {}

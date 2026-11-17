package com.spvb.tradingengine.domain;

import lombok.NonNull;

import java.math.BigDecimal;

public record QuoteLevel(
        @NonNull BigDecimal price,
        long quantity
) {}

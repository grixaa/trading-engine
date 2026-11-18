package com.spvb.tradingengine.domain;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record Balance(
        @NonNull String asset,
        @NonNull BigDecimal available,
        @NonNull BigDecimal reserved
) {}

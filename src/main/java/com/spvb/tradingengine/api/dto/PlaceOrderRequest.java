package com.spvb.tradingengine.api.dto;

import com.spvb.tradingengine.domain.Side;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

import static com.spvb.tradingengine.util.ApiConstants.ASSET_CODE_PATTERN;
import static java.lang.Integer.MAX_VALUE;

public record PlaceOrderRequest(
        @NotNull
        @Pattern(regexp = ASSET_CODE_PATTERN)
        String seccode,

        @NotNull Side side,

        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 8)
        BigDecimal price,

        @NotNull
        @Positive
        @Digits(integer = 10, fraction = 0)
        @Max(MAX_VALUE)
        BigDecimal quantity
) {}

package com.spvb.tradingengine.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

import static com.spvb.tradingengine.util.ApiConstants.ASSET_CODE_PATTERN;

public record DepositRequest(
        @NotNull
        @Pattern(regexp = ASSET_CODE_PATTERN)
        String asset,

        @NotNull
        @Positive
        @Digits(integer = 20, fraction = 8)
        BigDecimal amount
) {}

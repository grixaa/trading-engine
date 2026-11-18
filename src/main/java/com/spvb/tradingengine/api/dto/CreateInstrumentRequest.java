package com.spvb.tradingengine.api.dto;

import com.spvb.tradingengine.enums.InstrumentType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

import static com.spvb.tradingengine.util.ApiConstants.ASSET_CODE_PATTERN;
import static java.lang.Integer.MAX_VALUE;

public record CreateInstrumentRequest(
        @NotNull
        @Pattern(regexp = ASSET_CODE_PATTERN)
        String seccode,

        @NotNull InstrumentType type,

        @NotNull
        @Pattern(regexp = ASSET_CODE_PATTERN)
        String baseAsset,

        @NotNull
        @Pattern(regexp = ASSET_CODE_PATTERN)
        String quoteCurrency,

        @NotNull
        @Positive
        @Digits(integer = 10, fraction = 0)
        @Max(MAX_VALUE)
        BigDecimal lotSize,

        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 8)
        BigDecimal minStep
) {}

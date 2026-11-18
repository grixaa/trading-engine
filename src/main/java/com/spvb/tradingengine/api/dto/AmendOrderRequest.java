package com.spvb.tradingengine.api.dto;

import org.springframework.lang.Nullable;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

import static java.lang.Integer.MAX_VALUE;

public record AmendOrderRequest(
        @Nullable
        @Positive
        @Digits(integer = 12, fraction = 8)
        BigDecimal price,

        @Nullable
        @Positive
        @Digits(integer = 10, fraction = 0)
        @Max(MAX_VALUE)
        BigDecimal remaining
) {}

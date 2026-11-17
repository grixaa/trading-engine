package com.spvb.tradingengine.domain;

public record Execution(
        Order resting,
        int quantity
) {}

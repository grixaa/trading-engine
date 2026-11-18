package com.spvb.tradingengine.domain;

import lombok.NonNull;

public record Execution(
        @NonNull Order resting,
        int quantity
) {}

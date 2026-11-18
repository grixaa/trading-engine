package com.spvb.tradingengine.domain;

import lombok.NonNull;

import java.util.List;

public record MatchPlan(
        @NonNull List<Execution> executions,
        boolean cancelRemainder
) {}

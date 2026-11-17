package com.spvb.tradingengine.domain;

import java.util.List;

public record MatchPlan(
        List<Execution> executions,
        boolean cancelRemainder
) {}

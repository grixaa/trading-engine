package com.spvb.tradingengine.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.spvb.tradingengine.domain.Execution;
import com.spvb.tradingengine.domain.MatchPlan;
import com.spvb.tradingengine.domain.Order;
import lombok.NonNull;
import lombok.val;
import org.springframework.stereotype.Component;

@Component
class MatchingEngine {
    MatchPlan buildMatchPlan(
            final long userId,
            final int quantity,
            @NonNull final Iterator<Order> crossingInPriorityOrder
    ) {
        val executions = new ArrayList<Execution>();
        int remaining = quantity;

        while (remaining > 0 && crossingInPriorityOrder.hasNext()) {
            val resting = crossingInPriorityOrder.next();
            if (resting.userId() == userId) {
                return new MatchPlan(List.copyOf(executions), true);
            }
            val fill = Math.min(remaining, resting.remaining());
            executions.add(new Execution(resting, fill));
            remaining -= fill;
        }

        return new MatchPlan(List.copyOf(executions), false);
    }
}

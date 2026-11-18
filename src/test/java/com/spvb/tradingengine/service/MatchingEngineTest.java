package com.spvb.tradingengine.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import com.spvb.tradingengine.domain.Execution;
import com.spvb.tradingengine.domain.Order;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.junit.jupiter.api.Test;

import static com.spvb.tradingengine.enums.OrderStatus.OPEN;
import static com.spvb.tradingengine.enums.OrderStatus.PARTIALLY_FILLED;
import static com.spvb.tradingengine.enums.Side.SELL;
import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;

@FieldDefaults(level = PRIVATE, makeFinal = true)
class MatchingEngineTest {
    MatchingEngine matchingEngine = new MatchingEngine();

    @Test
    void buildsEmptyPlanWhenNoOppositeOrdersExist() {
        val plan = matchingEngine.buildMatchPlan(1, 3, Collections.emptyIterator());

        assertThat(plan.executions())
                .isEmpty();
        assertThat(plan.cancelRemainder())
                .isFalse();
    }

    @Test
    void usesOnlyOpenLotsAndStopsAfterFillingIncomingOrder() {
        val first = sellOrder(10, 2, 5, 3);
        val second = sellOrder(11, 3, 4, 0);
        val third = sellOrder(12, 4, 9, 0);

        val plan = matchingEngine.buildMatchPlan(
                1,
                5,
                List.of(first, second, third).iterator()
        );

        assertThat(plan.executions())
                .containsExactly(new Execution(first, 2), new Execution(second, 3));
        assertThat(plan.cancelRemainder())
                .isFalse();
    }

    @Test
    void leavesRemainderOpenWhenOppositeOrdersRunOut() {
        val resting = sellOrder(10, 2, 2, 0);

        val plan = matchingEngine.buildMatchPlan(1, 5, List.of(resting).iterator());

        assertThat(plan.executions())
                .containsExactly(new Execution(resting, 2));
        assertThat(plan.cancelRemainder())
                .isFalse();
    }

    @Test
    void preservesEarlierMatchesAndCancelsRemainderWhenItReachesOwnOrder() {
        val first = sellOrder(10, 2, 2, 0);
        val own = sellOrder(11, 1, 1, 0);
        val later = sellOrder(12, 3, 5, 0);

        val plan = matchingEngine.buildMatchPlan(1, 5, List.of(first, own, later).iterator());

        assertThat(plan.executions())
                .containsExactly(new Execution(first, 2));
        assertThat(plan.cancelRemainder())
                .isTrue();
    }

    private static Order sellOrder(
            final long id,
            final long userId,
            final int quantity,
            final int filled
    ) {
        val timestamp = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return Order.builder()
                .id(id)
                .userId(userId)
                .seccode("DEMO")
                .side(SELL)
                .price(BigDecimal.valueOf(100))
                .quantity(quantity)
                .filled(filled)
                .status(filled == 0 ? OPEN : PARTIALLY_FILLED)
                .priority(id)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
    }
}

package com.spvb.tradingengine;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.spvb.tradingengine.domain.Instrument;
import com.spvb.tradingengine.domain.Order;
import com.spvb.tradingengine.enums.OrderStatus;
import com.spvb.tradingengine.enums.Side;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import static com.spvb.tradingengine.enums.InstrumentType.STOCK;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TradingFixtures {
    public static final OffsetDateTime TIMESTAMP = OffsetDateTime.parse("2026-01-01T00:00:00Z");
    public static final BigDecimal PRICE = new BigDecimal("100.00");

    public static Instrument stockInstrument() {
        return Instrument.builder()
                .seccode("DEMO")
                .type(STOCK)
                .baseAsset("DEMO")
                .quoteCurrency("RUB")
                .lotSize(10)
                .minStep(new BigDecimal("0.01"))
                .build();
    }

    public static Order order(
            final long id,
            final long userId,
            @NonNull final Side side,
            @NonNull final OrderStatus status,
            final int filled
    ) {
        return order(id, userId, side, status, filled, PRICE);
    }

    public static Order order(
            final long id,
            final long userId,
            @NonNull final Side side,
            @NonNull final OrderStatus status,
            final int filled,
            @NonNull final BigDecimal price
    ) {
        return Order.builder()
                .id(id)
                .userId(userId)
                .seccode("DEMO")
                .side(side)
                .price(price)
                .quantity(2)
                .filled(filled)
                .status(status)
                .priority(id)
                .createdAt(TIMESTAMP)
                .updatedAt(TIMESTAMP)
                .build();
    }
}

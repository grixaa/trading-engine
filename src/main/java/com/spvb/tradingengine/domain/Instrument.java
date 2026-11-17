package com.spvb.tradingengine.domain;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.NonNull;

import static com.spvb.tradingengine.domain.Side.*;

@Builder
public record Instrument(
        @NonNull String seccode,
        @NonNull InstrumentType type,
        @NonNull String baseAsset,
        @NonNull String quoteCurrency,
        int lotSize,
        @NonNull BigDecimal minStep
) {
    public BigDecimal units(final int lots) {
        return BigDecimal.valueOf((long) lotSize * lots);
    }

    public String reservedAsset(@NonNull final Side side) {
        return side == BUY
                ? quoteCurrency
                : baseAsset;
    }

    public BigDecimal reservation(
            @NonNull final Side side,
            @NonNull final BigDecimal price,
            final int lots
    ) {
        return side == BUY
                ? price.multiply(units(lots))
                : units(lots);
    }
}

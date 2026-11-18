package com.spvb.tradingengine.domain;

import java.math.BigDecimal;

import com.spvb.tradingengine.enums.InstrumentType;
import com.spvb.tradingengine.enums.Side;
import lombok.Builder;
import lombok.NonNull;

import static com.spvb.tradingengine.enums.Side.*;

@Builder
public record Instrument(
        @NonNull String seccode,
        @NonNull InstrumentType type,
        @NonNull String baseAsset,
        @NonNull String quoteCurrency,
        int lotSize,
        @NonNull BigDecimal minStep
) {
    public BigDecimal calculateBaseUnits(final int lots) {
        return BigDecimal.valueOf((long) lotSize * lots);
    }

    public String getReserveAssetCode(@NonNull final Side side) {
        return side == BUY
                ? quoteCurrency
                : baseAsset;
    }

    public BigDecimal calculateRequiredReserve(
            @NonNull final Side side,
            @NonNull final BigDecimal price,
            final int lots
    ) {
        return side == BUY
                ? price.multiply(calculateBaseUnits(lots))
                : calculateBaseUnits(lots);
    }
}

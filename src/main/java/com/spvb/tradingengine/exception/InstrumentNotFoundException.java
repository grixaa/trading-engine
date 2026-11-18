package com.spvb.tradingengine.exception;

import lombok.experimental.StandardException;

@StandardException
public final class InstrumentNotFoundException extends RuntimeException implements TradingErrorDetails {
    private static final String CODE = "INSTRUMENT_NOT_FOUND";

    @Override
    public String getCode() {
        return CODE;
    }
}

package com.spvb.tradingengine.exception;

import lombok.experimental.StandardException;

@StandardException
public final class InstrumentExistsException extends RuntimeException implements TradingErrorDetails {
    private static final String CODE = "INSTRUMENT_EXISTS";

    @Override
    public String getCode() {
        return CODE;
    }
}

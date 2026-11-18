package com.spvb.tradingengine.exception;

import lombok.experimental.StandardException;

@StandardException
public final class EmptyAmendmentException extends RuntimeException implements TradingErrorDetails {
    private static final String CODE = "EMPTY_AMENDMENT";

    @Override
    public String getCode() {
        return CODE;
    }
}

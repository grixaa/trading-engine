package com.spvb.tradingengine.exception;

import lombok.experimental.StandardException;

@StandardException
public final class InvalidQuantityException extends RuntimeException implements TradingErrorDetails {
    private static final String CODE = "INVALID_QUANTITY";

    @Override
    public String getCode() {
        return CODE;
    }
}

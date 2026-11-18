package com.spvb.tradingengine.exception;

import lombok.experimental.StandardException;

@StandardException
public final class InvalidAmountException extends RuntimeException implements TradingErrorDetails {
    private static final String CODE = "INVALID_AMOUNT";

    @Override
    public String getCode() {
        return CODE;
    }
}

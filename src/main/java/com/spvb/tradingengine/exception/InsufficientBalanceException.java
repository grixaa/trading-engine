package com.spvb.tradingengine.exception;

import lombok.experimental.StandardException;

@StandardException
public final class InsufficientBalanceException extends RuntimeException implements TradingErrorDetails {
    private static final String CODE = "INSUFFICIENT_BALANCE";

    @Override
    public String getCode() {
        return CODE;
    }
}

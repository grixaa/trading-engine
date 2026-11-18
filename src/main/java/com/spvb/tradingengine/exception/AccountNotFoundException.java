package com.spvb.tradingengine.exception;

import lombok.experimental.StandardException;

@StandardException
public final class AccountNotFoundException extends RuntimeException implements TradingErrorDetails {
    private static final String CODE = "ACCOUNT_NOT_FOUND";

    @Override
    public String getCode() {
        return CODE;
    }
}

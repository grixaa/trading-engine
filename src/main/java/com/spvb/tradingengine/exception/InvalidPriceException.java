package com.spvb.tradingengine.exception;

import lombok.experimental.StandardException;

@StandardException
public final class InvalidPriceException extends RuntimeException implements TradingErrorDetails {
    private static final String CODE = "INVALID_PRICE";

    @Override
    public String getCode() {
        return CODE;
    }
}

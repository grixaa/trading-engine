package com.spvb.tradingengine.exception;

import lombok.experimental.StandardException;

@StandardException
public final class OrderNotFoundException extends RuntimeException implements TradingErrorDetails {
    private static final String CODE = "ORDER_NOT_FOUND";

    @Override
    public String getCode() {
        return CODE;
    }
}

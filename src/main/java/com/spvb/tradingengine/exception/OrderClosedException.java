package com.spvb.tradingengine.exception;

import lombok.experimental.StandardException;

@StandardException
public final class OrderClosedException extends RuntimeException implements TradingErrorDetails {
    private static final String CODE = "ORDER_CLOSED";

    @Override
    public String getCode() {
        return CODE;
    }
}

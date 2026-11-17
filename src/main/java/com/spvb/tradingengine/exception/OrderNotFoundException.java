package com.spvb.tradingengine.exception;

import lombok.Getter;
import lombok.experimental.StandardException;

@Getter
@StandardException
public final class OrderNotFoundException extends RuntimeException implements TradingErrorDetails {
    private final String code = "ORDER_NOT_FOUND";
}

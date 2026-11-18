package com.spvb.tradingengine.exception;

import org.springframework.lang.Nullable;

public interface TradingErrorDetails {
    String getCode();

    @Nullable
    String getMessage();
}

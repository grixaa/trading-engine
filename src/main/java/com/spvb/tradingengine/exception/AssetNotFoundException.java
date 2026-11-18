package com.spvb.tradingengine.exception;

import lombok.experimental.StandardException;

@StandardException
public final class AssetNotFoundException extends RuntimeException implements TradingErrorDetails {
    private static final String CODE = "ASSET_NOT_FOUND";

    @Override
    public String getCode() {
        return CODE;
    }
}

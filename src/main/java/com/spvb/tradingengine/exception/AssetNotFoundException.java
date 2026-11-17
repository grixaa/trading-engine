package com.spvb.tradingengine.exception;

import lombok.Getter;
import lombok.experimental.StandardException;

@Getter
@StandardException
public final class AssetNotFoundException extends RuntimeException implements TradingErrorDetails {
    private final String code = "ASSET_NOT_FOUND";
}

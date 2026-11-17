package com.spvb.tradingengine.exception;

import lombok.Getter;
import lombok.experimental.StandardException;

@Getter
@StandardException
public final class AccountNotFoundException extends RuntimeException implements TradingErrorDetails {
    private final String code = "ACCOUNT_NOT_FOUND";
}

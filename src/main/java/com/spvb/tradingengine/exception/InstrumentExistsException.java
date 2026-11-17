package com.spvb.tradingengine.exception;

import lombok.Getter;
import lombok.experimental.StandardException;

@Getter
@StandardException
public final class InstrumentExistsException extends RuntimeException implements TradingErrorDetails {
    private final String code = "INSTRUMENT_EXISTS";
}

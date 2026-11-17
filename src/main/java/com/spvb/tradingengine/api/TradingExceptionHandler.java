package com.spvb.tradingengine.api;

import com.spvb.tradingengine.exception.AccountNotFoundException;
import com.spvb.tradingengine.exception.AssetExistsException;
import com.spvb.tradingengine.exception.AssetNotFoundException;
import com.spvb.tradingengine.exception.EmptyAmendmentException;
import com.spvb.tradingengine.exception.InstrumentExistsException;
import com.spvb.tradingengine.exception.InstrumentNotFoundException;
import com.spvb.tradingengine.exception.InsufficientBalanceException;
import com.spvb.tradingengine.exception.InvalidAmountException;
import com.spvb.tradingengine.exception.InvalidInstrumentException;
import com.spvb.tradingengine.exception.InvalidPriceException;
import com.spvb.tradingengine.exception.InvalidQuantityException;
import com.spvb.tradingengine.exception.OrderClosedException;
import com.spvb.tradingengine.exception.OrderNotFoundException;
import com.spvb.tradingengine.exception.TradingErrorDetails;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ProblemDetail.forStatusAndDetail;

@RestControllerAdvice
@Slf4j
public class TradingExceptionHandler {
    @ExceptionHandler({
            AssetNotFoundException.class,
            InstrumentNotFoundException.class,
            OrderNotFoundException.class,
            AccountNotFoundException.class
    })
    public ResponseEntity<ProblemDetail> handleNotFound(@NonNull final TradingErrorDetails exception) {
        return response(NOT_FOUND, "Ресурс не найден", exception);
    }

    @ExceptionHandler({
            AssetExistsException.class,
            InstrumentExistsException.class,
            OrderClosedException.class,
            InsufficientBalanceException.class
    })
    public ResponseEntity<ProblemDetail> handleConflict(@NonNull final TradingErrorDetails exception) {
        return response(CONFLICT, "Конфликт состояния", exception);
    }

    @ExceptionHandler({
            InvalidInstrumentException.class,
            InvalidPriceException.class,
            InvalidQuantityException.class,
            InvalidAmountException.class,
            EmptyAmendmentException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(@NonNull final TradingErrorDetails exception) {
        return response(BAD_REQUEST, "Некорректный запрос", exception);
    }

    private ResponseEntity<ProblemDetail> response(
            @NonNull final HttpStatus status,
            @NonNull final String title,
            @NonNull final TradingErrorDetails exception
    ) {
        log.debug(
                "Запрос отклонён (HTTP {}, код {}): {}",
                status.value(),
                exception.getCode(),
                exception.getMessage()
        );

        val problem = forStatusAndDetail(status, exception.getMessage());

        problem.setTitle(title);
        problem.setProperty("code", exception.getCode());

        return ResponseEntity
                .status(status)
                .body(problem);
    }
}

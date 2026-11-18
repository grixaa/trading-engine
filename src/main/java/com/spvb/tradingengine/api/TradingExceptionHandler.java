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
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ProblemDetail.forStatusAndDetail;

@RestControllerAdvice
@Slf4j
public class TradingExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    @Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull final MethodArgumentNotValidException exception,
            @NonNull final HttpHeaders headers,
            @NonNull final HttpStatusCode status,
            @NonNull final WebRequest request
    ) {
        val violations = exception.getBindingResult()
                .getAllErrors().stream()
                .map(error -> describeViolation(error.getObjectName(), error))
                .toList();

        log.debug("Запрос не прошёл валидацию, нарушения={}", violations);
        return super.handleMethodArgumentNotValid(exception, headers, status, request);
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull final HandlerMethodValidationException exception,
            @NonNull final HttpHeaders headers,
            @NonNull final HttpStatusCode status,
            @NonNull final WebRequest request
    ) {
        val violations = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> describeViolation(result.getMethodParameter().getParameterName(), error)))
                .toList();
        if (exception.isForReturnValue()) {
            log.warn("Ответ контроллера не прошёл валидацию, нарушения={}", violations);
        } else {
            log.debug("Запрос не прошёл валидацию, нарушения={}", violations);
        }
        return super.handleHandlerMethodValidationException(exception, headers, status, request);
    }

    private static String describeViolation(
            @Nullable final String parameterName,
            @NonNull final MessageSourceResolvable error
    ) {
        val field = error instanceof FieldError fieldError
                ? fieldError.getField()
                : parameterName;
        val codes = error.getCodes();
        val rule = codes == null || codes.length == 0
                ? "неизвестно"
                : codes[codes.length - 1];
        return "%s (%s)".formatted(field == null ? "параметр" : field, rule);
    }

    @ExceptionHandler({
            AssetNotFoundException.class,
            InstrumentNotFoundException.class,
            OrderNotFoundException.class,
            AccountNotFoundException.class
    })
    public ResponseEntity<ProblemDetail> handleNotFound(@NonNull final TradingErrorDetails exception) {
        return buildProblemResponse(NOT_FOUND, "Ресурс не найден", exception);
    }

    @ExceptionHandler({
            AssetExistsException.class,
            InstrumentExistsException.class,
            OrderClosedException.class,
            InsufficientBalanceException.class
    })
    public ResponseEntity<ProblemDetail> handleConflict(@NonNull final TradingErrorDetails exception) {
        return buildProblemResponse(CONFLICT, "Конфликт состояния", exception);
    }

    @ExceptionHandler({
            InvalidInstrumentException.class,
            InvalidPriceException.class,
            InvalidQuantityException.class,
            InvalidAmountException.class,
            EmptyAmendmentException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(@NonNull final TradingErrorDetails exception) {
        return buildProblemResponse(BAD_REQUEST, "Некорректный запрос", exception);
    }

    private ResponseEntity<ProblemDetail> buildProblemResponse(
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

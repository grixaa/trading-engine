package com.spvb.tradingengine.api;

import com.spvb.tradingengine.exception.InvalidPriceException;
import com.spvb.tradingengine.exception.InsufficientBalanceException;
import com.spvb.tradingengine.exception.OrderNotFoundException;
import lombok.NonNull;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class TradingExceptionHandlerTest {
    @Test
    void mapsBusinessErrorsToHttpStatusAndPreservesCodeAndMessage() {
        val handler = new TradingExceptionHandler();

        assertProblem(
                handler.handleNotFound(new OrderNotFoundException("Заявка не найдена")),
                404,
                "ORDER_NOT_FOUND",
                "Заявка не найдена"
        );
        assertProblem(
                handler.handleConflict(new InsufficientBalanceException("Недостаточно средств")),
                409,
                "INSUFFICIENT_BALANCE",
                "Недостаточно средств"
        );
        assertProblem(
                handler.handleBadRequest(new InvalidPriceException("Некорректная цена")),
                400,
                "INVALID_PRICE",
                "Некорректная цена"
        );
    }

    private static void assertProblem(
            @NonNull final ResponseEntity<ProblemDetail> response,
            final int status,
            @NonNull final String code,
            @NonNull final String message
    ) {
        assertThat(response.getStatusCode().value())
                .isEqualTo(status);
        val problem = response.getBody();
        assertThat(problem)
                .isNotNull();
        assertThat(problem.getDetail())
                .isEqualTo(message);
        assertThat(problem.getProperties())
                .containsEntry("code", code);
    }
}

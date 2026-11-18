package com.spvb.tradingengine.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.slf4j.event.Level.*;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;

@Component
@Order(HIGHEST_PRECEDENCE)
@Slf4j
class ApiRequestLoggingFilter extends OncePerRequestFilter {
    private static final String REQUEST_ID = "requestId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected boolean shouldNotFilter(@NonNull final HttpServletRequest request) {
        return !request.getRequestURI()
                .startsWith("%s/api/".formatted(request.getContextPath()));
    }

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain chain
    ) throws ServletException, IOException {
        val previousRequestId = MDC.get(REQUEST_ID);
        val requestId = UUID.randomUUID().toString();
        val started = System.nanoTime();

        MDC.put(REQUEST_ID, requestId);
        boolean completed = false;

        try {
            response.setHeader(REQUEST_ID_HEADER, requestId);
            chain.doFilter(request, response);
            completed = true;
        } finally {
            try {
                logRequest(
                        request,
                        completed
                                ? response.getStatus()
                                : SC_INTERNAL_SERVER_ERROR,
                        NANOSECONDS.toMillis(System.nanoTime() - started)
                );
            } finally {
                if (previousRequestId == null) {
                    MDC.remove(REQUEST_ID);
                } else {
                    MDC.put(REQUEST_ID, previousRequestId);
                }
            }
        }
    }

    private void logRequest(
            @NonNull final HttpServletRequest request,
            final int status,
            final long durationMs
    ) {
        val method = request.getMethod();
        val route = request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE);

        log.atLevel(resolveLogLevel(method, status))
                .log(
                        "HTTP-запрос завершён, метод={}, маршрут={}, статус={}, длительностьМс={}",
                        method,
                        route,
                        status,
                        durationMs
                );
    }

    private static Level resolveLogLevel(@NonNull final String method, final int status) {
        if (status >= SC_INTERNAL_SERVER_ERROR) {
            return WARN;
        }
        if (status >= SC_BAD_REQUEST || "GET".equals(method) || "HEAD".equals(method)) {
            return DEBUG;
        }
        return INFO;
    }
}

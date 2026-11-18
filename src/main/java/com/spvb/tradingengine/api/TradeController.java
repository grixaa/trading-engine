package com.spvb.tradingengine.api;

import org.springframework.lang.Nullable;

import com.spvb.tradingengine.domain.CursorPage;
import com.spvb.tradingengine.domain.Trade;
import com.spvb.tradingengine.domain.TradeFilter;
import com.spvb.tradingengine.service.MarketDataService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.spvb.tradingengine.util.ApiConstants.ASSET_CODE_PATTERN;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class TradeController {
    MarketDataService marketDataService;

    @GetMapping
    public ResponseEntity<CursorPage<Trade>> listTrades(
            @RequestParam(required = false)
            @Pattern(regexp = ASSET_CODE_PATTERN)
            @Nullable
            final String seccode,

            @RequestParam(required = false)
            @Positive
            @Nullable
            final Long orderId,

            @RequestParam(required = false)
            @Positive
            @Nullable
            final Long beforeId,

            @RequestParam(defaultValue = "20")
            @Positive
            @Max(100)
            final int limit
    ) {
        return ResponseEntity
                .ok(marketDataService.listTrades(
                        TradeFilter.builder()
                                .seccode(seccode)
                                .orderId(orderId)
                                .beforeId(beforeId)
                                .limit(limit)
                                .build()
                ));
    }
}

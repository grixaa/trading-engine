package com.spvb.tradingengine.api;

import java.util.List;

import com.spvb.tradingengine.domain.CursorPage;
import com.spvb.tradingengine.domain.Trade;
import com.spvb.tradingengine.domain.TradeFilter;
import com.spvb.tradingengine.service.MarketDataService;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = PRIVATE)
class TradeControllerTest {
    @Mock MarketDataService marketDataService;
    @InjectMocks TradeController controller;

    @Test
    void forwardsAllFiltersAndReturnsServicePage() {
        val filter = TradeFilter.builder()
                .seccode("DEMO")
                .orderId(7L)
                .beforeId(12L)
                .limit(5)
                .build();
        val trades = new CursorPage<Trade>(List.of(), null);
        when(marketDataService.listTrades(filter))
                .thenReturn(trades);

        val response = controller.listTrades("DEMO", 7L, 12L, 5);

        assertThat(response.getStatusCode().value())
                .isEqualTo(200);
        assertThat(response.getBody())
                .isEqualTo(trades);
    }
}

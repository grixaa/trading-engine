package com.spvb.tradingengine.api;

import com.spvb.tradingengine.api.dto.CreateAssetRequest;
import com.spvb.tradingengine.api.dto.CreateInstrumentRequest;
import com.spvb.tradingengine.domain.Asset;
import com.spvb.tradingengine.domain.OrderBook;
import com.spvb.tradingengine.service.CatalogService;
import com.spvb.tradingengine.service.MarketDataService;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static com.spvb.tradingengine.TradingFixtures.stockInstrument;
import static com.spvb.tradingengine.enums.AssetType.CURRENCY;
import static com.spvb.tradingengine.enums.InstrumentType.STOCK;
import static java.math.BigDecimal.TEN;
import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = PRIVATE)
class CatalogControllerTest {
    @Mock CatalogService catalogService;
    @Mock MarketDataService marketDataService;
    @InjectMocks CatalogController controller;

    @Test
    void createsAssetAndExposesItsLocation() {
        val asset = Asset.builder()
                .code("RUB")
                .type(CURRENCY)
                .build();
        when(catalogService.createAsset(asset))
                .thenReturn(asset);

        val response = controller.createAsset(new CreateAssetRequest("RUB", CURRENCY));

        assertThat(response.getStatusCode().value())
                .isEqualTo(201);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("/api/assets/RUB"));
        assertThat(response.getBody())
                .isEqualTo(asset);
    }

    @Test
    void passesInstrumentFieldsAndReturnsCreatedResource() {
        val instrument = stockInstrument();
        when(catalogService.createInstrument(instrument))
                .thenReturn(instrument);

        val request = new CreateInstrumentRequest(
                "DEMO",
                STOCK,
                "DEMO",
                "RUB",
                TEN,
                instrument.minStep()
        );
        val response = controller.createInstrument(request);

        assertThat(response.getStatusCode().value())
                .isEqualTo(201);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("/api/instruments/DEMO"));
        assertThat(response.getBody())
                .isEqualTo(instrument);
    }

    @Test
    void returnsCatalogResources() {
        val asset = Asset.builder()
                .code("RUB")
                .type(CURRENCY)
                .build();
        val instrument = stockInstrument();
        when(catalogService.getAssetByCode("RUB"))
                .thenReturn(asset);
        when(catalogService.listAssets())
                .thenReturn(List.of(asset));
        when(catalogService.getInstrumentByCode("DEMO"))
                .thenReturn(instrument);
        when(catalogService.listInstruments())
                .thenReturn(List.of(instrument));

        assertThat(controller.getAssetByCode("RUB").getBody())
                .isEqualTo(asset);
        assertThat(controller.listAssets().getBody())
                .containsExactly(asset);
        assertThat(controller.getInstrumentByCode("DEMO").getBody())
                .isEqualTo(instrument);
        assertThat(controller.listInstruments().getBody())
                .containsExactly(instrument);
    }

    @Test
    void returnsOrderBookAtRequestedDepth() {
        val book = new OrderBook("DEMO", List.of(), List.of());
        when(marketDataService.getOrderBook("DEMO", 3))
                .thenReturn(book);

        val response = controller.getOrderBook("DEMO", 3);

        assertThat(response.getStatusCode().value())
                .isEqualTo(200);
        assertThat(response.getBody())
                .isEqualTo(book);
    }
}

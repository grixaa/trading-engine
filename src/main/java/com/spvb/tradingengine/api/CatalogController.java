package com.spvb.tradingengine.api;

import lombok.NonNull;

import com.spvb.tradingengine.api.dto.CreateAssetRequest;
import com.spvb.tradingengine.api.dto.CreateInstrumentRequest;
import com.spvb.tradingengine.domain.Asset;
import com.spvb.tradingengine.domain.Instrument;
import com.spvb.tradingengine.domain.OrderBook;
import com.spvb.tradingengine.service.CatalogService;
import com.spvb.tradingengine.service.MarketDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static com.spvb.tradingengine.util.ApiConstants.ASSET_CODE_PATTERN;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class CatalogController {
    CatalogService catalogService;
    MarketDataService marketDataService;

    @PostMapping("/assets")
    public ResponseEntity<Asset> createAsset(
            @Valid @RequestBody @NonNull final CreateAssetRequest request
    ) {
        val asset = catalogService.createAsset(Asset.builder()
                .code(request.code())
                .type(request.type())
                .build());

        return ResponseEntity
                .created(URI.create("/api/assets/%s".formatted(asset.code())))
                .body(asset);
    }

    @GetMapping("/assets")
    public ResponseEntity<List<Asset>> assets() {
        return ResponseEntity
                .ok(catalogService.assets());
    }

    @GetMapping("/assets/{code}")
    public ResponseEntity<Asset> asset(
            @PathVariable
            @Pattern(regexp = ASSET_CODE_PATTERN)
            @NonNull
            final String code
    ) {
        return ResponseEntity
                .ok(catalogService.asset(code));
    }

    @PostMapping("/instruments")
    public ResponseEntity<Instrument> createInstrument(
            @Valid
            @RequestBody
            @NonNull
            final CreateInstrumentRequest request
    ) {
        val instrument = catalogService.createInstrument(Instrument.builder()
                .seccode(request.seccode())
                .type(request.type())
                .baseAsset(request.baseAsset())
                .quoteCurrency(request.quoteCurrency())
                .lotSize(request.lotSize().intValueExact())
                .minStep(request.minStep())
                .build());

        return ResponseEntity
                .created(URI.create("/api/instruments/%s".formatted(instrument.seccode())))
                .body(instrument);
    }

    @GetMapping("/instruments")
    public ResponseEntity<List<Instrument>> instruments() {
        return ResponseEntity
                .ok(catalogService.instruments());
    }

    @GetMapping("/instruments/{seccode}")
    public ResponseEntity<Instrument> instrument(
            @PathVariable
            @Pattern(regexp = ASSET_CODE_PATTERN)
            @NonNull
            final String seccode
    ) {
        return ResponseEntity
                .ok(catalogService.instrument(seccode));
    }

    @GetMapping("/instruments/{seccode}/order-book")
    public ResponseEntity<OrderBook> orderBook(
            @PathVariable
            @Pattern(regexp = ASSET_CODE_PATTERN)
            @NonNull final
            String seccode,

            @RequestParam(defaultValue = "10")
            @Positive
            @Max(100)
            final int depth
    ) {
        return ResponseEntity
                .ok(marketDataService.book(seccode, depth));
    }
}

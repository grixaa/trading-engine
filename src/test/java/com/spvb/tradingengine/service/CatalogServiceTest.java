package com.spvb.tradingengine.service;

import java.util.Optional;

import com.spvb.tradingengine.domain.Asset;
import com.spvb.tradingengine.domain.Instrument;
import com.spvb.tradingengine.entity.AssetEntity;
import com.spvb.tradingengine.exception.AssetExistsException;
import com.spvb.tradingengine.exception.InstrumentExistsException;
import com.spvb.tradingengine.exception.InstrumentNotFoundException;
import com.spvb.tradingengine.exception.InvalidInstrumentException;
import com.spvb.tradingengine.repository.AssetRepository;
import com.spvb.tradingengine.repository.InstrumentRepository;

import static com.spvb.tradingengine.TradingFixtures.stockInstrument;
import static com.spvb.tradingengine.enums.AssetType.*;
import static com.spvb.tradingengine.enums.InstrumentType.FX;

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
class CatalogServiceTest {
    @Mock
    AssetRepository assetRepository;
    @Mock
    InstrumentRepository instrumentRepository;
    @Mock
    AssetEntity baseAsset;
    @Mock
    AssetEntity quoteAsset;
    @InjectMocks
    CatalogService catalogService;

    @Test
    void rejectsDuplicateAsset() {
        assertThatThrownBy(() -> catalogService.createAsset(new Asset("RUB", CURRENCY)))
                .isInstanceOf(AssetExistsException.class);
        verify(assetRepository).insertIfAbsent("RUB", "CURRENCY");
    }

    @Test
    void createsStockWithSecurityBaseAndCurrencyQuote() {
        givenStockAssets();
        val instrument = stockInstrument();
        when(instrumentRepository.insertIfAbsent(
                "DEMO",
                "STOCK",
                "DEMO",
                "RUB",
                10,
                instrument.minStep()
        )).thenReturn(1);

        assertThat(catalogService.createInstrument(instrument))
                .isEqualTo(instrument);
    }

    @Test
    void rejectsExistingInstrument() {
        givenStockAssets();

        assertThatThrownBy(() -> catalogService.createInstrument(stockInstrument()))
                .isInstanceOf(InstrumentExistsException.class);
    }

    @Test
    void rejectsCurrencyPairWithSameBaseAndQuote() {
        when(assetRepository.findById("RUB"))
                .thenReturn(Optional.of(quoteAsset));
        when(quoteAsset.getCode())
                .thenReturn("RUB");
        when(quoteAsset.getType())
                .thenReturn(CURRENCY);
        val instrument = Instrument.builder()
                .seccode("RUBRUB")
                .type(FX)
                .baseAsset("RUB")
                .quoteCurrency("RUB")
                .lotSize(1)
                .minStep(stockInstrument().minStep())
                .build();

        assertThatThrownBy(() -> catalogService.createInstrument(instrument))
                .isInstanceOf(InvalidInstrumentException.class);
        verifyNoInteractions(instrumentRepository);
    }

    @Test
    void reportsMissingInstrumentWhenTryingToAcquireTradingLock() {
        assertThatThrownBy(() -> catalogService.lockInstrumentForTrading("DEMO"))
                .isInstanceOf(InstrumentNotFoundException.class);
        verify(instrumentRepository).findLockedBySeccode("DEMO");
    }

    private void givenStockAssets() {
        when(assetRepository.findById("DEMO"))
                .thenReturn(Optional.of(baseAsset));
        when(baseAsset.getCode())
                .thenReturn("DEMO");
        when(baseAsset.getType())
                .thenReturn(SECURITY);
        when(assetRepository.findById("RUB"))
                .thenReturn(Optional.of(quoteAsset));
        when(quoteAsset.getCode())
                .thenReturn("RUB");
        when(quoteAsset.getType())
                .thenReturn(CURRENCY);
    }
}

package com.spvb.tradingengine.service;

import com.spvb.tradingengine.domain.Asset;
import com.spvb.tradingengine.domain.Instrument;
import com.spvb.tradingengine.exception.*;
import com.spvb.tradingengine.repository.AssetRepository;
import com.spvb.tradingengine.repository.InstrumentRepository;
import com.spvb.tradingengine.repository.mapper.PersistenceMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.spvb.tradingengine.domain.AssetType.CURRENCY;
import static com.spvb.tradingengine.domain.AssetType.SECURITY;
import static com.spvb.tradingengine.domain.InstrumentType.FX;
import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class CatalogService {
    AssetRepository assetRepository;
    InstrumentRepository instrumentRepository;

    @Transactional
    public Asset createAsset(@NonNull final Asset asset) {
        if (assetRepository.insertIfAbsent(asset.code(), asset.type().name()) == 0) {
            throw new AssetExistsException("Актив %s уже существует".formatted(asset.code()));
        }
        return asset;
    }

    public Asset asset(@NonNull final String code) {
        return assetRepository.findById(code)
                .map(PersistenceMapper::toAsset)
                .orElseThrow(() -> new AssetNotFoundException("Актив %s не найден".formatted(code)));
    }

    public List<Asset> assets() {
        return assetRepository.findAllByOrderByCodeAsc().stream()
                .map(PersistenceMapper::toAsset)
                .toList();
    }

    @Transactional
    public Instrument createInstrument(@NonNull final Instrument instrument) {
        val base = asset(instrument.baseAsset());
        val quote = asset(instrument.quoteCurrency());
        val expectedBaseType = instrument.type() == FX
                ? CURRENCY
                : SECURITY;

        if (base.type() != expectedBaseType ||
                quote.type() != CURRENCY ||
                !StringUtils.equals(base.code(), quote.code())
        ) {
            throw new InvalidInstrumentException("Несовместимые типы базового актива и валюты котировки");
        }

        val inserted = instrumentRepository.insertIfAbsent(
                instrument.seccode(),
                instrument.type()
                        .name(),
                instrument.baseAsset(),
                instrument.quoteCurrency(),
                instrument.lotSize(),
                instrument.minStep()
        );

        if (inserted == 0) {
            throw new InstrumentExistsException("Инструмент %s уже существует".formatted(instrument.seccode()));
        }

        return instrument;
    }

    public Instrument instrument(@NonNull final String code) {
        return instrumentRepository.findById(code)
                .map(PersistenceMapper::toInstrument)
                .orElseThrow(() -> new InstrumentNotFoundException("Инструмент %s не найден".formatted(code)));
    }

    public List<Instrument> instruments() {
        return instrumentRepository.findAllByOrderBySeccodeAsc().stream()
                .map(PersistenceMapper::toInstrument)
                .toList();
    }

    public Instrument lockInstrument(@NonNull final String code) {
        return instrumentRepository.findLockedBySeccode(code)
                .map(PersistenceMapper::toInstrument)
                .orElseThrow(() -> new InstrumentNotFoundException("Инструмент %s не найден".formatted(code)));
    }
}

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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.spvb.tradingengine.enums.AssetType.CURRENCY;
import static com.spvb.tradingengine.enums.AssetType.SECURITY;
import static com.spvb.tradingengine.enums.InstrumentType.FX;
import static com.spvb.tradingengine.util.TransactionLogging.logAfterCommit;
import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
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
        logAfterCommit(() -> log.info("Актив зарегистрирован, код={}, тип={}", asset.code(), asset.type()));
        return asset;
    }

    public Asset getAssetByCode(@NonNull final String code) {
        return assetRepository.findById(code)
                .map(PersistenceMapper::toAsset)
                .orElseThrow(() -> new AssetNotFoundException("Актив %s не найден".formatted(code)));
    }

    public List<Asset> listAssets() {
        return assetRepository.findAllByOrderByCodeAsc().stream()
                .map(PersistenceMapper::toAsset)
                .toList();
    }

    @Transactional
    public Instrument createInstrument(@NonNull final Instrument instrument) {
        val base = getAssetByCode(instrument.baseAsset());
        val quote = getAssetByCode(instrument.quoteCurrency());
        val expectedBaseType = instrument.type() == FX
                ? CURRENCY
                : SECURITY;

        if (base.type() != expectedBaseType ||
                quote.type() != CURRENCY ||
                StringUtils.equals(base.code(), quote.code())
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

        logAfterCommit(() -> log.info(
                "Инструмент зарегистрирован, код={}, тип={}",
                instrument.seccode(),
                instrument.type()
        ));
        return instrument;
    }

    public Instrument getInstrumentByCode(@NonNull final String code) {
        return instrumentRepository.findById(code)
                .map(PersistenceMapper::toInstrument)
                .orElseThrow(() -> new InstrumentNotFoundException("Инструмент %s не найден".formatted(code)));
    }

    public List<Instrument> listInstruments() {
        return instrumentRepository.findAllByOrderBySeccodeAsc().stream()
                .map(PersistenceMapper::toInstrument)
                .toList();
    }

    public Instrument lockInstrumentForTrading(@NonNull final String code) {
        return instrumentRepository.findLockedBySeccode(code)
                .map(PersistenceMapper::toInstrument)
                .orElseThrow(() -> new InstrumentNotFoundException("Инструмент %s не найден".formatted(code)));
    }
}

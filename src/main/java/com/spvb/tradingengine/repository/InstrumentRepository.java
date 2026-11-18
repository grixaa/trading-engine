package com.spvb.tradingengine.repository;

import com.spvb.tradingengine.entity.InstrumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface InstrumentRepository extends JpaRepository<InstrumentEntity, String> {
    List<InstrumentEntity> findAllByOrderBySeccodeAsc();

    @Lock(PESSIMISTIC_WRITE)
    Optional<InstrumentEntity> findLockedBySeccode(String seccode);

    @Modifying
    @Query(value = """
            INSERT INTO instrument (seccode, type, base_asset, quote_currency, lot_size, min_step)
            VALUES (:seccode, :type, :baseAsset, :quoteCurrency, :lotSize, :minStep)
            ON CONFLICT (seccode) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            String seccode,
            String type,
            String baseAsset,
            String quoteCurrency,
            int lotSize,
            BigDecimal minStep
    );
}

package com.spvb.tradingengine.repository;

import java.util.List;

import com.spvb.tradingengine.entity.TradeEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TradeRepository extends JpaRepository<TradeEntity, Long> {
    @Query("""
            SELECT t FROM TradeEntity t
            WHERE (:seccode IS NULL OR t.seccode = :seccode)
              AND (:orderId IS NULL OR t.buyOrderId = :orderId OR t.sellOrderId = :orderId)
              AND (:beforeId IS NULL OR t.id < :beforeId)
            ORDER BY t.id DESC
            """)
    List<TradeEntity> search(String seccode, Long orderId, Long beforeId, Pageable page);
}

package com.spvb.tradingengine.repository;

import java.util.List;

import com.spvb.tradingengine.entity.BalanceEntity;
import com.spvb.tradingengine.entity.BalanceId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceRepository extends JpaRepository<BalanceEntity, BalanceId> {
    List<BalanceEntity> findByIdUserIdOrderByIdAsset(long userId);
}

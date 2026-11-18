package com.spvb.tradingengine.repository;

import java.util.List;

import com.spvb.tradingengine.entity.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AssetRepository extends JpaRepository<AssetEntity, String> {
    List<AssetEntity> findAllByOrderByCodeAsc();

    @Modifying
    @Query(value = """
            INSERT INTO asset (code, type) VALUES (:code, :type)
            ON CONFLICT (code) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(String code, String type);
}

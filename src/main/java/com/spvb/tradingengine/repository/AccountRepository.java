package com.spvb.tradingengine.repository;

import com.spvb.tradingengine.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    @Modifying
    @Query(value = """
            INSERT INTO trading_account (user_id) VALUES (:userId)
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    void open(long userId);

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.userId IN :users ORDER BY a.userId")
    List<AccountEntity> lockBatch(Collection<Long> users);
}

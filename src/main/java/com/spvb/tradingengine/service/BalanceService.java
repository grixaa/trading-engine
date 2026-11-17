package com.spvb.tradingengine.service;

import java.math.BigDecimal;

import com.spvb.tradingengine.domain.Balance;
import com.spvb.tradingengine.repository.BalanceRepository;
import com.spvb.tradingengine.entity.BalanceEntity;
import com.spvb.tradingengine.entity.BalanceId;
import com.spvb.tradingengine.repository.mapper.PersistenceMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Service;

import static lombok.AccessLevel.PRIVATE;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
class BalanceService {
    BalanceRepository balanceRepository;

    public boolean move(
            final long userId,
            @NonNull final String asset,
            @NonNull final BigDecimal availableDelta,
            @NonNull final BigDecimal reservedDelta
    ) {
        val existing = balanceRepository.findById(new BalanceId(userId, asset));

        if (existing.isEmpty()) {
            return false;
        }

        val balance = existing.get();
        val nextAvailable = balance.getAvailable().add(availableDelta);
        val nextReserved = balance.getReserved().add(reservedDelta);

        if (nextAvailable.signum() < 0 || nextReserved.signum() < 0) {
            return false;
        }

        balance.setAvailable(nextAvailable);
        balance.setReserved(nextReserved);

        return true;
    }

    public Balance credit(
            final long userId,
            @NonNull final String asset,
            @NonNull final BigDecimal amount
    ) {
        val id = new BalanceId(userId, asset);
        val existing = balanceRepository.findById(id);

        if (existing.isEmpty()) {
            val balance = BalanceEntity.builder()
                    .id(id)
                    .available(amount)
                    .reserved(BigDecimal.ZERO)
                    .build();

            return PersistenceMapper.toBalance(balanceRepository.save(balance));
        }

        val balance = existing.get();
        balance.setAvailable(balance.getAvailable().add(amount));
        return PersistenceMapper.toBalance(balance);
    }
}

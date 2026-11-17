package com.spvb.tradingengine.service;

import com.spvb.tradingengine.domain.Balance;
import com.spvb.tradingengine.exception.AccountNotFoundException;
import com.spvb.tradingengine.exception.InvalidAmountException;
import com.spvb.tradingengine.repository.AccountRepository;
import com.spvb.tradingengine.repository.BalanceRepository;
import com.spvb.tradingengine.repository.mapper.PersistenceMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.spvb.tradingengine.domain.AssetType.SECURITY;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class AccountService {
    CatalogService catalogService;
    BalanceService balanceService;
    AccountRepository accountRepository;
    BalanceRepository balanceRepository;

    @Transactional
    public void open(final long userId) {
        accountRepository.open(userId);
    }

    public List<Balance> balances(final long userId) {
        if (!accountRepository.existsById(userId)) {
            throw new AccountNotFoundException("Торговый счёт участника %d не найден".formatted(userId));
        }
        return balanceRepository.findByIdUserIdOrderByIdAsset(userId).stream()
                .map(PersistenceMapper::toBalance)
                .toList();
    }

    @Transactional
    public Balance deposit(
            final long userId,
            @NonNull final String assetCode,
            @NonNull final BigDecimal amount
    ) {
        val asset = catalogService.asset(assetCode);

        if (asset.type() == SECURITY && amount.stripTrailingZeros().scale() > 0) {
            throw new InvalidAmountException("Количество ценных бумаг должно быть целым числом");
        }

        if (isEmpty(accountRepository.lockBatch(List.of(userId)))) {
            throw new AccountNotFoundException("Торговый счёт участника %d не найден".formatted(userId));
        }

        return balanceService.credit(userId, assetCode, amount);
    }
}

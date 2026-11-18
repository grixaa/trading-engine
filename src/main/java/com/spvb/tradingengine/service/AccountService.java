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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.spvb.tradingengine.enums.AssetType.SECURITY;
import static com.spvb.tradingengine.util.TransactionLogging.logAfterCommit;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class AccountService {
    CatalogService catalogService;
    BalanceService balanceService;
    AccountRepository accountRepository;
    BalanceRepository balanceRepository;

    @Transactional
    public void openTradingAccount(final long userId) {
        accountRepository.insertAccountIfAbsent(userId);
        logAfterCommit(() -> log.info("Запрос на открытие счёта выполнен, участникId={}", userId));
    }

    public List<Balance> getAccountBalances(final long userId) {
        if (!accountRepository.existsById(userId)) {
            throw new AccountNotFoundException("Торговый счёт участника %d не найден".formatted(userId));
        }
        return balanceRepository.findByIdUserIdOrderByIdAsset(userId).stream()
                .map(PersistenceMapper::toBalance)
                .toList();
    }

    @Transactional
    public Balance depositAsset(
            final long userId,
            @NonNull final String assetCode,
            @NonNull final BigDecimal amount
    ) {
        val asset = catalogService.getAssetByCode(assetCode);

        if (asset.type() == SECURITY && amount.stripTrailingZeros().scale() > 0) {
            throw new InvalidAmountException("Количество ценных бумаг должно быть целым числом");
        }

        if (isEmpty(accountRepository.lockAccountsByUserIds(List.of(userId)))) {
            throw new AccountNotFoundException("Торговый счёт участника %d не найден".formatted(userId));
        }

        val balance = balanceService.creditAvailableBalance(userId, assetCode, amount);
        logAfterCommit(() -> log.info("Актив зачислен на счёт, участникId={}, актив={}", userId, assetCode));
        return balance;
    }
}

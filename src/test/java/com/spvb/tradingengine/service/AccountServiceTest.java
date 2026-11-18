package com.spvb.tradingengine.service;

import java.math.BigDecimal;
import java.util.List;

import com.spvb.tradingengine.domain.Asset;
import com.spvb.tradingengine.domain.Balance;
import com.spvb.tradingengine.entity.AccountEntity;
import com.spvb.tradingengine.entity.BalanceEntity;
import com.spvb.tradingengine.entity.BalanceId;
import com.spvb.tradingengine.exception.AccountNotFoundException;
import com.spvb.tradingengine.exception.InvalidAmountException;
import com.spvb.tradingengine.repository.AccountRepository;
import com.spvb.tradingengine.repository.BalanceRepository;

import static com.spvb.tradingengine.enums.AssetType.*;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.math.BigDecimal.*;
import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = PRIVATE)
class AccountServiceTest {
    @Mock CatalogService catalogService;
    @Mock BalanceService balanceService;
    @Mock AccountRepository accountRepository;
    @Mock BalanceRepository balanceRepository;
    @InjectMocks AccountService accountService;

    @Test
    void opensAccountUsingIdempotentInsert() {
        accountService.openTradingAccount(42);
        verify(accountRepository).insertAccountIfAbsent(42);
    }

    @Test
    void mapsBalancesForExistingAccount() {
        val entity = BalanceEntity.builder()
                .id(new BalanceId(42, "RUB"))
                .available(TEN)
                .reserved(ONE)
                .build();
        when(accountRepository.existsById(42L))
                .thenReturn(true);
        when(balanceRepository.findByIdUserIdOrderByIdAsset(42))
                .thenReturn(List.of(entity));

        assertThat(accountService.getAccountBalances(42))
                .containsExactly(new Balance("RUB", TEN, ONE));
    }

    @Test
    void failsBeforeReadingBalancesForUnknownAccount() {
        assertThatThrownBy(() -> accountService.getAccountBalances(42))
                .isInstanceOf(AccountNotFoundException.class);
        verifyNoInteractions(balanceRepository);
    }

    @Test
    void locksAccountBeforeCreditingFractionalCurrency() {
        val amount = new BigDecimal("10.25");
        val credited = new Balance("RUB", amount, ZERO);
        when(catalogService.getAssetByCode("RUB"))
                .thenReturn(new Asset("RUB", CURRENCY));
        when(accountRepository.lockAccountsByUserIds(List.of(42L)))
                .thenReturn(List.of(new AccountEntity()));
        when(balanceService.creditAvailableBalance(42, "RUB", amount))
                .thenReturn(credited);

        assertThat(accountService.depositAsset(42, "RUB", amount))
                .isEqualTo(credited);
        val calls = inOrder(accountRepository, balanceService);
        calls.verify(accountRepository)
                .lockAccountsByUserIds(List.of(42L));
        calls.verify(balanceService)
                .creditAvailableBalance(42, "RUB", amount);
    }

    @Test
    void rejectsFractionalSecurityBeforeLockingOrCreditingAccount() {
        when(catalogService.getAssetByCode("DEMO"))
                .thenReturn(new Asset("DEMO", SECURITY));

        assertThatThrownBy(() -> accountService.depositAsset(42, "DEMO", new BigDecimal("1.5")))
                .isInstanceOf(InvalidAmountException.class);
        verifyNoInteractions(accountRepository, balanceService);
    }
}

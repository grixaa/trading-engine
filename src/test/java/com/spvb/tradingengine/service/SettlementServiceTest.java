package com.spvb.tradingengine.service;

import java.math.BigDecimal;
import java.util.List;

import com.spvb.tradingengine.domain.Execution;
import com.spvb.tradingengine.domain.MatchPlan;
import com.spvb.tradingengine.entity.AccountEntity;
import com.spvb.tradingengine.exception.AccountNotFoundException;
import com.spvb.tradingengine.exception.InsufficientBalanceException;
import com.spvb.tradingengine.repository.AccountRepository;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.spvb.tradingengine.TradingFixtures.*;
import static com.spvb.tradingengine.enums.OrderStatus.OPEN;
import static com.spvb.tradingengine.enums.Side.*;
import static java.math.BigDecimal.TEN;
import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = PRIVATE)
class SettlementServiceTest {
    @Mock BalanceService balanceService;
    @Mock AccountRepository accountRepository;
    @Mock AccountEntity buyerAccount;
    @Mock AccountEntity sellerAccount;
    @InjectMocks SettlementService settlementService;

    @Test
    void locksDistinctParticipantsInAscendingUserIdOrder() {
        val sellerOrder = order(7, 7, SELL, OPEN, 0);
        when(accountRepository.lockAccountsByUserIds(List.of(7L, 42L)))
                .thenReturn(List.of(sellerAccount, buyerAccount));
        when(buyerAccount.getUserId())
                .thenReturn(42L);
        when(sellerAccount.getUserId())
                .thenReturn(7L);

        settlementService.lockParticipantAccounts(
                42,
                new MatchPlan(List.of(new Execution(sellerOrder, 1), new Execution(sellerOrder, 1)), false)
        );

        verify(accountRepository).lockAccountsByUserIds(List.of(7L, 42L));
    }

    @Test
    void reportsMissingIncomingAccount() {
        val plan = new MatchPlan(List.of(), false);

        assertThatThrownBy(() -> settlementService.lockParticipantAccounts(42, plan))
                .isInstanceOf(AccountNotFoundException.class);
        verifyNoInteractions(balanceService);
    }

    @Test
    void rejectsReserveIncreaseWhenAvailableFundsAreInsufficient() {
        assertThatThrownBy(() -> settlementService.adjustAssetReservation(42, "RUB", TEN))
                .isInstanceOf(InsufficientBalanceException.class);
        verify(balanceService)
                .adjustBalanceAmounts(42, "RUB", TEN.negate(), TEN);
    }

    @Test
    void transfersAssetsAtRestingPriceAndReturnsPriceImprovementToBuyer() {
        val buy = order(8, 42, BUY, OPEN, 0, new BigDecimal("105.00"));
        val sell = order(7, 43, SELL, OPEN, 0);
        val refund = new BigDecimal("50.00");
        val quoteReserveDebit = new BigDecimal("-1050.00");
        when(balanceService.adjustBalanceAmounts(42, "RUB", refund, quoteReserveDebit))
                .thenReturn(true);
        when(balanceService.adjustBalanceAmounts(43, "DEMO", BigDecimal.ZERO, TEN.negate()))
                .thenReturn(true);

        settlementService.settleExecution(stockInstrument(), buy, new Execution(sell, 1));

        val calls = inOrder(balanceService);
        calls.verify(balanceService)
                .adjustBalanceAmounts(42, "RUB", refund, quoteReserveDebit);
        calls.verify(balanceService)
                .adjustBalanceAmounts(43, "DEMO", BigDecimal.ZERO, TEN.negate());
        calls.verify(balanceService)
                .creditAvailableBalance(42, "DEMO", TEN);
        calls.verify(balanceService)
                .creditAvailableBalance(43, "RUB", new BigDecimal("1000.00"));
    }
}

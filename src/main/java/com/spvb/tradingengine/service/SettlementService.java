package com.spvb.tradingengine.service;

import lombok.NonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.spvb.tradingengine.domain.Execution;
import com.spvb.tradingengine.domain.Instrument;
import com.spvb.tradingengine.domain.MatchPlan;
import com.spvb.tradingengine.domain.Order;
import com.spvb.tradingengine.domain.Side;
import com.spvb.tradingengine.exception.AccountNotFoundException;
import com.spvb.tradingengine.exception.InsufficientBalanceException;
import com.spvb.tradingengine.repository.AccountRepository;
import com.spvb.tradingengine.entity.AccountEntity;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.stereotype.Component;

import static lombok.AccessLevel.PRIVATE;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
class SettlementService {
    private static final int ACCOUNT_LOCK_BATCH_SIZE = 1000;

    BalanceService balanceService;
    AccountRepository accountRepository;

    void lockParticipants(final long userId, @NonNull final MatchPlan plan) {
        val participants = new TreeSet<Long>();
        participants.add(userId);
        for (val execution : plan.executions()) {
            participants.add(execution.resting().userId());
        }
        val locked = lockAccounts(List.copyOf(participants));
        if (!locked.contains(userId)) {
            throw new AccountNotFoundException("Торговый счёт участника %d не найден".formatted(userId));
        }
        if (locked.size() != participants.size()) {
            throw new IllegalStateException("Не найден торговый счёт встречной заявки");
        }
    }

    private List<Long> lockAccounts(@NonNull final List<Long> sortedUsers) {
        val locked = new ArrayList<Long>(sortedUsers.size());
        for (int start = 0; start < sortedUsers.size(); start += ACCOUNT_LOCK_BATCH_SIZE) {
            val end = Math.min(start + ACCOUNT_LOCK_BATCH_SIZE, sortedUsers.size());
            accountRepository.lockBatch(sortedUsers.subList(start, end)).stream()
                    .map(AccountEntity::getUserId).forEach(locked::add);
        }
        return locked;
    }

    void changeReservation(final long userId, @NonNull final String asset, @NonNull final BigDecimal delta) {
        if (delta.signum() == 0) {
            return;
        }
        if (balanceService.move(userId, asset, delta.negate(), delta)) {
            return;
        }
        if (delta.signum() > 0) {
            throw new InsufficientBalanceException("Недостаточный доступный остаток актива %s".formatted(asset));
        }
        throw new IllegalStateException("Не найден резерв актива %s на счёте участника %d".formatted(asset, userId));
    }

    void settle(@NonNull final Instrument instrument, @NonNull final Order incoming, @NonNull final Execution execution) {
        val resting = execution.resting();
        val buy = incoming.side() == Side.BUY ? incoming : resting;
        val sell = incoming.side() == Side.SELL ? incoming : resting;
        val units = instrument.units(execution.quantity());
        val cost = resting.price().multiply(units);
        val reservedCash = buy.price().multiply(units);

        consumeReserve(buy.userId(), instrument.quoteCurrency(), reservedCash.subtract(cost), reservedCash);
        consumeReserve(sell.userId(), instrument.baseAsset(), BigDecimal.ZERO, units);
        balanceService.credit(buy.userId(), instrument.baseAsset(), units);
        balanceService.credit(sell.userId(), instrument.quoteCurrency(), cost);
    }

    private void consumeReserve(final long userId, @NonNull final String asset, @NonNull final BigDecimal refund, @NonNull final BigDecimal consumed) {
        if (!balanceService.move(userId, asset, refund, consumed.negate())) {
            throw new IllegalStateException("Не найден резерв актива %s на счёте участника %d".formatted(asset, userId));
        }
    }
}

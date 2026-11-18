package com.spvb.tradingengine.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.spvb.tradingengine.entity.BalanceEntity;
import com.spvb.tradingengine.entity.BalanceId;
import com.spvb.tradingengine.repository.BalanceRepository;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.math.BigDecimal.TEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {
    private static final long USER_ID = 42;
    private static final String ASSET = "RUB";

    private static final BalanceId ID = new BalanceId(USER_ID, ASSET);

    @Mock
    private BalanceRepository repository;

    @InjectMocks
    private BalanceService balanceService;

    @Test
    void transfersAvailableAmountIntoReserve() {
        val balance = givenBalance(100, 20);

        assertThat(balanceService.adjustBalanceAmounts(
                USER_ID, ASSET, BigDecimal.valueOf(-30), BigDecimal.valueOf(30)))
                .isTrue();
        assertThat(balance.getAvailable())
                .isEqualByComparingTo("70");
        assertThat(balance.getReserved())
                .isEqualByComparingTo("50");
    }

    @Test
    void leavesBothAmountsIntactWhenEitherWouldBecomeNegative() {
        val balance = givenBalance(100, 20);

        assertThat(balanceService.adjustBalanceAmounts(
                USER_ID,
                ASSET,
                BigDecimal.valueOf(-101),
                BigDecimal.valueOf(10))
        ).isFalse();
        assertThat(balanceService.adjustBalanceAmounts(
                USER_ID,
                ASSET,
                TEN,
                BigDecimal.valueOf(-21))
        ).isFalse();
        assertThat(balance.getAvailable())
                .isEqualByComparingTo("100");
        assertThat(balance.getReserved())
                .isEqualByComparingTo("20");
    }

    @Test
    void rejectsAdjustmentForMissingBalance() {
        when(repository.findById(ID))
                .thenReturn(Optional.empty());

        assertThat(balanceService.adjustBalanceAmounts(USER_ID, ASSET, TEN.negate(), TEN))
                .isFalse();
        verify(repository, never())
                .save(any(BalanceEntity.class));
    }

    @Test
    void creditsExistingBalanceWithoutChangingReserve() {
        givenBalance(100, 20);

        val updated = balanceService.creditAvailableBalance(USER_ID, ASSET, TEN);

        assertThat(updated.available())
                .isEqualByComparingTo("110");
        assertThat(updated.reserved())
                .isEqualByComparingTo("20");
        verify(repository, never())
                .save(any(BalanceEntity.class));
    }

    @Test
    void createsBalanceWithZeroReserveOnFirstCredit() {
        when(repository.findById(ID))
                .thenReturn(Optional.empty());
        when(repository.save(any(BalanceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        val created = balanceService.creditAvailableBalance(USER_ID, ASSET, new BigDecimal("12.50"));

        assertThat(created.asset())
                .isEqualTo(ASSET);
        assertThat(created.available())
                .isEqualByComparingTo("12.50");
        assertThat(created.reserved())
                .isZero();
        verify(repository)
                .save(any(BalanceEntity.class));
    }

    private BalanceEntity givenBalance(final long available, final long reserved) {
        val balance = BalanceEntity.builder()
                .id(ID)
                .available(BigDecimal.valueOf(available))
                .reserved(BigDecimal.valueOf(reserved))
                .build();
        when(repository.findById(ID))
                .thenReturn(Optional.of(balance));
        return balance;
    }
}

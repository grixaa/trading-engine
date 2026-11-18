package com.spvb.tradingengine.api;

import java.math.BigDecimal;
import java.util.List;

import com.spvb.tradingengine.api.dto.DepositRequest;
import com.spvb.tradingengine.domain.Balance;
import com.spvb.tradingengine.service.AccountService;
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
class AccountControllerTest {
    @Mock AccountService accountService;
    @InjectMocks AccountController controller;

    @Test
    void opensAccountAndReturnsNoContent() {
        assertThat(controller.openTradingAccount(42).getStatusCode().value())
                .isEqualTo(204);
        verify(accountService).openTradingAccount(42);
    }

    @Test
    void returnsUpdatedBalanceAfterDeposit() {
        val balance = Balance.builder()
                .asset("RUB")
                .available(TEN)
                .reserved(ZERO)
                .build();

        when(accountService.depositAsset(42, "RUB", TEN))
                .thenReturn(balance);

        val response = controller.depositAsset(42, new DepositRequest("RUB", TEN));

        assertThat(response.getStatusCode().value())
                .isEqualTo(200);
        assertThat(response.getBody())
                .isEqualTo(balance);
    }

    @Test
    void returnsBalancesOfRequestedAccount() {
        val balance = Balance.builder()
                .asset("RUB")
                .available(TEN)
                .reserved(ZERO)
                .build();
        when(accountService.getAccountBalances(42))
                .thenReturn(List.of(balance));

        val response = controller.getAccountBalances(42);

        assertThat(response.getStatusCode().value())
                .isEqualTo(200);
        assertThat(response.getBody())
                .containsExactly(balance);
    }
}

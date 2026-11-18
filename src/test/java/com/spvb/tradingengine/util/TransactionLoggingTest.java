package com.spvb.tradingengine.util;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import static com.spvb.tradingengine.util.TransactionLogging.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED;
import static org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;
import static org.springframework.transaction.support.TransactionSynchronizationUtils.*;
import static org.springframework.transaction.support.TransactionSynchronizationUtils.triggerAfterCommit;

class TransactionLoggingTest {
    @BeforeEach
    void startTransactionSynchronization() {
        setActualTransactionActive(true);
        initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        clear();
    }

    @Test
    void logsSuccessOnlyAfterCommit() {
        val logged = new AtomicBoolean();

        logAfterCommit(() -> logged.set(true));

        assertThat(logged).isFalse();

        triggerAfterCommit();
        triggerAfterCompletion(STATUS_COMMITTED);

        assertThat(logged)
                .isTrue();
    }

    @Test
    void doesNotLogSuccessAfterRollback() {
        val logged = new AtomicBoolean();
        logAfterCommit(() -> logged.set(true));

        triggerAfterCompletion(STATUS_ROLLED_BACK);

        assertThat(logged)
                .isFalse();
    }
}

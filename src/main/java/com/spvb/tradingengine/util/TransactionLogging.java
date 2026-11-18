package com.spvb.tradingengine.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.springframework.transaction.support.TransactionSynchronization;

import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@UtilityClass
public class TransactionLogging {
    public static void logAfterCommit(@NonNull final Runnable logAction) {
        if (!isSynchronizationActive() || !isActualTransactionActive()) {
            return;
        }

        registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                logAction.run();
            }
        });
    }
}

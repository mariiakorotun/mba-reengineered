package com.mini_banking_app.ledger.api;

import com.mini_banking_app.account.api.MovementType;
import com.mini_banking_app.shared.domain.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Запис журналу операцій для показу. Поля currency та balanceAfter можуть бути null для записів першої версії. */
public record TransactionView(Long id,
                              LocalDateTime createdAt,
                              MovementType type,
                              Long accountId,
                              BigDecimal amount,
                              Currency currency,
                              BigDecimal balanceAfter,
                              Long actorId,
                              String reference) {

    public boolean isCredit() {
        return type != null && type.isCredit();
    }
}

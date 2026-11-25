package com.mini_banking_app.account.api;

import com.mini_banking_app.shared.domain.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Доменна подія: на рахунку змінився баланс.
 * Публікується в межах транзакції БД; модуль ledger записує її в журнал у тій самій транзакції.
 */
public record MoneyMovedEvent(Long accountId,
                              Long actorId,
                              MovementType type,
                              BigDecimal amount,
                              Currency currency,
                              BigDecimal balanceAfter,
                              String reference,
                              LocalDateTime occurredAt) {
}

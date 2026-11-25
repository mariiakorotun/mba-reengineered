package com.mini_banking_app.web;

import com.mini_banking_app.ledger.api.TransactionView;
import com.mini_banking_app.shared.domain.Currency;

/** Рядок таблиці історії: запис журналу, доповнений даними інших модулів. */
public record TransactionRow(TransactionView tx, String accountLabel, Currency currency, String actor) {
}

package com.mini_banking_app.ledger.api;

import java.util.Collection;
import java.util.List;

/** Запити до журналу операцій. */
public interface LedgerQueries {

    /** Максимальна кількість записів на сторінці загальної історії. */
    int PAGE_LIMIT = 500;

    List<TransactionView> historyOf(Collection<Long> accountIds);

    List<TransactionView> latest();
}

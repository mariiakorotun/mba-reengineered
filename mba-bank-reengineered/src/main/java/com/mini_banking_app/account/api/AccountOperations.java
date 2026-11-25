package com.mini_banking_app.account.api;

import com.mini_banking_app.shared.domain.Currency;

import java.math.BigDecimal;

/** Команди (зміна стану) модуля account. Усі перевірки прав виконуються всередині модуля. */
public interface AccountOperations {

    AccountView open(Long ownerId, AccountKind kind, Currency currency, String actorUsername);

    AccountView deposit(Long accountId, BigDecimal amount, String actorUsername);

    AccountView withdraw(Long accountId, BigDecimal amount, String actorUsername);

    /** Переказ між власними рахунками однієї валюти. */
    void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount, String actorUsername);

    /** Закриття рахунку (м'яке видалення) — лише з нульовим балансом. */
    void close(Long accountId, String reason, String actorUsername);
}

package com.mini_banking_app.account.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Запити (читання) модуля account. */
public interface AccountQueries {

    List<AccountView> activeAccountsOf(String username);

    /** Усі рахунки користувача, включно із закритими (для історії операцій). */
    List<AccountView> allAccountsOf(String username);

    /** Активні рахунки всіх інших користувачів (моніторинг для адміністратора). */
    List<AccountView> activeAccountsExcept(String username);

    /** Рахунок, доступний власнику або адміністратору. */
    AccountView accountFor(Long accountId, String username);

    Map<Long, AccountView> byIds(Collection<Long> ids);
}

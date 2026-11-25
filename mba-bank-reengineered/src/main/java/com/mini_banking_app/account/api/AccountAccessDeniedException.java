package com.mini_banking_app.account.api;

import com.mini_banking_app.shared.domain.DomainException;

/** Спроба працювати з чужим рахунком (у першій версії не перевірялася для операцій з коштами). */
public class AccountAccessDeniedException extends DomainException {

    public AccountAccessDeniedException() {
        super("Доступ заборонено: рахунок належить іншому користувачу");
    }
}

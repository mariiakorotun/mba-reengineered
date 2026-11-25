package com.mini_banking_app.account.api;

import com.mini_banking_app.shared.domain.DomainException;

public class AccountNotFoundException extends DomainException {

    public AccountNotFoundException(Long id) {
        super("Рахунок не знайдено (id " + id + ")");
    }
}

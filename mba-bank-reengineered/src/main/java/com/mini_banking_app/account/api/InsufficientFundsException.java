package com.mini_banking_app.account.api;

import com.mini_banking_app.shared.domain.DomainException;

public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException() {
        super("Недостатньо коштів на рахунку");
    }
}

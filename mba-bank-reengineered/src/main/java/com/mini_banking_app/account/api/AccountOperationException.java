package com.mini_banking_app.account.api;

import com.mini_banking_app.shared.domain.DomainException;

/** Операція неможлива в поточному стані рахунку (закритий, ненульовий баланс, інша валюта тощо). */
public class AccountOperationException extends DomainException {

    public AccountOperationException(String message) {
        super(message);
    }
}

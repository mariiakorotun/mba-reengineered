package com.mini_banking_app.user.api;

import com.mini_banking_app.shared.domain.DomainException;

public class UserOperationNotAllowedException extends DomainException {

    public UserOperationNotAllowedException(String message) {
        super(message);
    }
}

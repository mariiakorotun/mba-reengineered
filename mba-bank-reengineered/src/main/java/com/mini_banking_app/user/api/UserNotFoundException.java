package com.mini_banking_app.user.api;

import com.mini_banking_app.shared.domain.DomainException;

public class UserNotFoundException extends DomainException {

    public UserNotFoundException(String who) {
        super("Користувача не знайдено: " + who);
    }
}

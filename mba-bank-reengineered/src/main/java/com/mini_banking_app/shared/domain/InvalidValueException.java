package com.mini_banking_app.shared.domain;

/** Некоректне значення на вході (сума, валюта тощо). */
public class InvalidValueException extends DomainException {

    public InvalidValueException(String message) {
        super(message);
    }
}

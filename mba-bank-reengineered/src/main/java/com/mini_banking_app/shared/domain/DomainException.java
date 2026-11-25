package com.mini_banking_app.shared.domain;

/**
 * Базовий виняток порушення бізнес-правила.
 * Повідомлення призначене для показу користувачу, тому не містить технічних деталей.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}

package com.mini_banking_app.account.application;

import com.mini_banking_app.account.domain.Account;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Порт сховища рахунків (гексагональна архітектура).
 * Сервіс залежить від цього інтерфейсу, а не від Spring Data, тому його можна
 * тестувати зі сховищем у пам'яті.
 */
public interface AccountStore {

    Optional<Account> findById(Long id);

    /** Читання з блокуванням рядка (SELECT ... FOR UPDATE) для операцій з коштами. */
    Optional<Account> lockById(Long id);

    List<Account> findActiveByOwner(Long ownerId);

    List<Account> findAllByOwner(Long ownerId);

    List<Account> findActiveByOwnerNot(Long ownerId);

    List<Account> findAllById(Collection<Long> ids);

    long countByOwner(Long ownerId);

    Account save(Account account);
}

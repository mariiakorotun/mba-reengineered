package com.mini_banking_app.account;

import com.mini_banking_app.account.application.AccountStore;
import com.mini_banking_app.account.domain.Account;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Сховище рахунків у пам'яті — тестова реалізація порту AccountStore. */
class InMemoryAccountStore implements AccountStore {

    private final Map<Long, Account> data = new LinkedHashMap<>();
    private long sequence = 0;

    @Override
    public Optional<Account> findById(Long id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public Optional<Account> lockById(Long id) {
        return findById(id);
    }

    @Override
    public List<Account> findActiveByOwner(Long ownerId) {
        return data.values().stream().filter(a -> a.isOwnedBy(ownerId) && a.isActive()).toList();
    }

    @Override
    public List<Account> findAllByOwner(Long ownerId) {
        return data.values().stream().filter(a -> a.isOwnedBy(ownerId)).toList();
    }

    @Override
    public List<Account> findActiveByOwnerNot(Long ownerId) {
        return data.values().stream().filter(a -> !a.isOwnedBy(ownerId) && a.isActive()).toList();
    }

    @Override
    public List<Account> findAllById(Collection<Long> ids) {
        return ids.stream().map(data::get).filter(Objects::nonNull).toList();
    }

    @Override
    public long countByOwner(Long ownerId) {
        return findAllByOwner(ownerId).size();
    }

    @Override
    public Account save(Account account) {
        if (account.getId() == null) {
            assignId(account, ++sequence);
        }
        data.put(account.getId(), account);
        return account;
    }

    /** Імітація генерації первинного ключа базою даних. */
    private static void assignId(Account account, long id) {
        try {
            Field field = Account.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

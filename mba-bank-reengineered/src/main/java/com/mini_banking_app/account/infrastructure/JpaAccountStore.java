package com.mini_banking_app.account.infrastructure;

import com.mini_banking_app.account.application.AccountStore;
import com.mini_banking_app.account.domain.Account;
import com.mini_banking_app.account.domain.AccountStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Адаптер порту {@link AccountStore} на Spring Data JPA. */
@Component
class JpaAccountStore implements AccountStore {

    private final AccountJpaRepository repository;

    JpaAccountStore(AccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Account> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Account> lockById(Long id) {
        return repository.lockById(id);
    }

    @Override
    public List<Account> findActiveByOwner(Long ownerId) {
        return repository.findByOwnerIdAndStatusOrderByIdAsc(ownerId, AccountStatus.ACTIVE);
    }

    @Override
    public List<Account> findAllByOwner(Long ownerId) {
        return repository.findByOwnerIdOrderByIdAsc(ownerId);
    }

    @Override
    public List<Account> findActiveByOwnerNot(Long ownerId) {
        return repository.findByOwnerIdNotAndStatusOrderByIdAsc(ownerId, AccountStatus.ACTIVE);
    }

    @Override
    public List<Account> findAllById(Collection<Long> ids) {
        return repository.findAllById(ids);
    }

    @Override
    public long countByOwner(Long ownerId) {
        return repository.countByOwnerId(ownerId);
    }

    @Override
    public Account save(Account account) {
        return repository.save(account);
    }
}

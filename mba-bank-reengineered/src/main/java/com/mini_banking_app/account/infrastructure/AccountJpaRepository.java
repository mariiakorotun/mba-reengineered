package com.mini_banking_app.account.infrastructure;

import com.mini_banking_app.account.domain.Account;
import com.mini_banking_app.account.domain.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface AccountJpaRepository extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> lockById(@Param("id") Long id);

    List<Account> findByOwnerIdAndStatusOrderByIdAsc(Long ownerId, AccountStatus status);

    List<Account> findByOwnerIdOrderByIdAsc(Long ownerId);

    List<Account> findByOwnerIdNotAndStatusOrderByIdAsc(Long ownerId, AccountStatus status);

    long countByOwnerId(Long ownerId);
}

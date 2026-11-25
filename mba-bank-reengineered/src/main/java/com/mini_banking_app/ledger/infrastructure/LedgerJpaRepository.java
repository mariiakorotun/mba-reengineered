package com.mini_banking_app.ledger.infrastructure;

import com.mini_banking_app.ledger.domain.LedgerEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LedgerJpaRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByAccountIdInOrderByCreatedAtDescIdDesc(Collection<Long> accountIds);

    List<LedgerEntry> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}

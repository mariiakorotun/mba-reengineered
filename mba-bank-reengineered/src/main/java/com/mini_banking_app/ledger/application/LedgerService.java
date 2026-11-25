package com.mini_banking_app.ledger.application;

import com.mini_banking_app.account.api.MoneyMovedEvent;
import com.mini_banking_app.ledger.api.LedgerQueries;
import com.mini_banking_app.ledger.api.TransactionView;
import com.mini_banking_app.ledger.domain.LedgerEntry;
import com.mini_banking_app.ledger.infrastructure.LedgerJpaRepository;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Журнал операцій. Модуль не викликається модулем account напряму —
 * він підписаний на подію {@link MoneyMovedEvent}.
 */
@Service
public class LedgerService implements LedgerQueries {

    private final LedgerJpaRepository entries;

    public LedgerService(LedgerJpaRepository entries) {
        this.entries = entries;
    }

    /**
     * Синхронний обробник: виконується в тій самій транзакції, що й зміна балансу
     * (Propagation.MANDATORY гарантує, що поза транзакцією запис не відбудеться).
     * Якщо запис у журнал не вдасться, відкотиться і зміна балансу — атомарність збережено.
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(MoneyMovedEvent event) {
        entries.save(LedgerEntry.from(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionView> historyOf(Collection<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return entries.findByAccountIdInOrderByCreatedAtDescIdDesc(accountIds).stream()
                .map(LedgerService::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionView> latest() {
        return entries.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, PAGE_LIMIT)).stream()
                .map(LedgerService::toView).toList();
    }

    private static TransactionView toView(LedgerEntry e) {
        return new TransactionView(e.getId(), e.getCreatedAt(), e.getType(), e.getAccountId(),
                e.getAmount(), e.getCurrency(), e.getBalanceAfter(), e.getActorId(), e.getReference());
    }
}

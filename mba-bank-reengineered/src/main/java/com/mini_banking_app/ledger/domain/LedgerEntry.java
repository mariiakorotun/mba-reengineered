package com.mini_banking_app.ledger.domain;

import com.mini_banking_app.account.api.MoneyMovedEvent;
import com.mini_banking_app.account.api.MovementType;
import com.mini_banking_app.shared.domain.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Незмінний запис журналу операцій (таблиця {@code transactions}).
 * Записи ніколи не видаляються: у першій версії видалення рахунку стирало всю його історію.
 */
@Entity
@Table(name = "transactions", indexes = @Index(name = "ix_transactions_account_created", columnList = "account_id, created_at"))
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "NVARCHAR(50)")
    private MovementType type;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    /** Хто виконав операцію. Стовпець user_id збережено з першої версії. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 3, updatable = false)
    private Currency currency;

    @Column(name = "balance_after", precision = 19, scale = 2, updatable = false)
    private BigDecimal balanceAfter;

    /** Спільний ідентифікатор двох записів одного переказу. */
    @Column(name = "reference", length = 36, updatable = false)
    private String reference;

    protected LedgerEntry() {
    }

    public static LedgerEntry from(MoneyMovedEvent e) {
        LedgerEntry entry = new LedgerEntry();
        entry.type = e.type();
        entry.amount = e.amount();
        entry.createdAt = e.occurredAt();
        entry.accountId = e.accountId();
        entry.actorId = e.actorId();
        entry.currency = e.currency();
        entry.balanceAfter = e.balanceAfter();
        entry.reference = e.reference();
        return entry;
    }

    public Long getId() {
        return id;
    }

    public MovementType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getActorId() {
        return actorId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getReference() {
        return reference;
    }
}

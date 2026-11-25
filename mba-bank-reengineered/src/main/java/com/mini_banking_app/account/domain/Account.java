package com.mini_banking_app.account.domain;

import com.mini_banking_app.account.api.AccountKind;
import com.mini_banking_app.account.api.AccountOperationException;
import com.mini_banking_app.account.api.InsufficientFundsException;
import com.mini_banking_app.shared.domain.Amount;
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
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Агрегат «Рахунок» (насичена доменна модель).
 * <p>
 * У першій версії сутність була лише контейнером даних, а правила (перевірка залишку,
 * заборона операцій над чужим чи закритим рахунком) жили в сервісі або були відсутні.
 * Тепер усі інваріанти рахунку зосереджені тут і перевіряються модульними тестами без Spring і БД.
 * <p>
 * Схема таблиці {@code accounts} розширена адитивно: нові стовпці мають значення за замовчуванням,
 * тож наявні рядки залишаються коректними.
 */
@Entity
@Table(name = "accounts", indexes = @Index(name = "ix_accounts_owner_status", columnList = "user_id, status"))
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Назва для показу. Стовпець "type" збережено для сумісності з першою версією. */
    @Column(name = "type", columnDefinition = "NVARCHAR(50)")
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", columnDefinition = "NVARCHAR(20) DEFAULT 'CURRENT' NOT NULL")
    private AccountKind kind;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private Currency currency;

    /** Власник — посилання за ідентифікатором, без JPA-зв'язку з модулем user. */
    @Column(name = "user_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "NVARCHAR(20) DEFAULT 'ACTIVE' NOT NULL")
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** Оптимістичне блокування — додатковий захист від втрачених оновлень. */
    @Version
    @Column(name = "version", columnDefinition = "BIGINT DEFAULT 0 NOT NULL")
    private Long version;

    protected Account() {
    }

    private Account(Long ownerId, AccountKind kind, Currency currency, String label) {
        this.ownerId = ownerId;
        this.kind = kind;
        this.currency = currency;
        this.label = label;
    }

    /**
     * Відкриття рахунку. Порядковий номер рахує й закриті рахунки, тому номери не повторюються
     * (у першій версії після видалення рахунку номер видавався повторно).
     */
    public static Account open(Long ownerId, AccountKind kind, Currency currency, long sequenceNumber) {
        if (ownerId == null || kind == null || currency == null) {
            throw new AccountOperationException("Неповні дані для відкриття рахунку");
        }
        String label = kind.title() + " #" + sequenceNumber + " (" + currency.name() + ")";
        return new Account(ownerId, kind, currency, label);
    }

    public void deposit(Amount amount) {
        requireActive();
        balance = balance.add(amount.value());
    }

    public void withdraw(Amount amount) {
        requireActive();
        if (balance.compareTo(amount.value()) < 0) {
            throw new InsufficientFundsException();
        }
        balance = balance.subtract(amount.value());
    }

    /** Закриття можливе лише за нульового балансу; історія операцій зберігається. */
    public void close(LocalDateTime now) {
        requireActive();
        if (balance.signum() != 0) {
            throw new AccountOperationException(
                    "Рахунок має ненульовий баланс. Спершу зніміть або перекажіть кошти");
        }
        status = AccountStatus.CLOSED;
        closedAt = now;
    }

    public void requireSameCurrency(Account other) {
        if (currency != other.currency) {
            throw new AccountOperationException("Переказ можливий лише між рахунками однієї валюти");
        }
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    private void requireActive() {
        if (!isActive()) {
            throw new AccountOperationException("Рахунок закрито — операції неможливі");
        }
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public AccountKind getKind() {
        return kind;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}

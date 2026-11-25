package com.mini_banking_app.account;

import com.mini_banking_app.account.api.AccountKind;
import com.mini_banking_app.account.api.AccountOperationException;
import com.mini_banking_app.account.api.InsufficientFundsException;
import com.mini_banking_app.account.domain.Account;
import com.mini_banking_app.account.domain.AccountStatus;
import com.mini_banking_app.shared.domain.Amount;
import com.mini_banking_app.shared.domain.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Модульні тести агрегату «Рахунок» — без Spring і без бази даних. */
class AccountTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 10, 0);

    private static Account account() {
        return Account.open(1L, AccountKind.CURRENT, Currency.UAH, 1);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "очікувалось " + expected + ", отримано " + actual);
    }

    @Test
    @DisplayName("Новий рахунок активний, з нульовим балансом і зрозумілою назвою")
    void opensActiveWithZeroBalance() {
        Account a = Account.open(7L, AccountKind.SAVINGS, Currency.EUR, 3);
        assertTrue(a.isActive());
        assertMoney("0", a.getBalance());
        assertEquals("Ощадний #3 (EUR)", a.getLabel());
        assertTrue(a.isOwnedBy(7L));
        assertFalse(a.isOwnedBy(8L));
    }

    @Test
    @DisplayName("Поповнення і зняття змінюють баланс")
    void depositAndWithdraw() {
        Account a = account();
        a.deposit(Amount.of("100.50"));
        a.withdraw(Amount.of("0.50"));
        assertMoney("100.00", a.getBalance());
    }

    @Test
    @DisplayName("Зняття понад залишок неможливе, баланс не змінюється")
    void cannotOverdraw() {
        Account a = account();
        a.deposit(Amount.of("50"));
        assertThrows(InsufficientFundsException.class, () -> a.withdraw(Amount.of("50.01")));
        assertMoney("50.00", a.getBalance());
    }

    @Test
    @DisplayName("Рахунок з ненульовим балансом закрити не можна")
    void cannotCloseWithMoney() {
        Account a = account();
        a.deposit(Amount.of("1"));
        assertThrows(AccountOperationException.class, () -> a.close(NOW));
        assertTrue(a.isActive());
    }

    @Test
    @DisplayName("Закриття з нульовим балансом — м'яке: статус CLOSED і дата")
    void closesSoftly() {
        Account a = account();
        a.close(NOW);
        assertEquals(AccountStatus.CLOSED, a.getStatus());
        assertEquals(NOW, a.getClosedAt());
    }

    @Test
    @DisplayName("Над закритим рахунком операції заборонені")
    void closedAccountIsFrozen() {
        Account a = account();
        a.close(NOW);
        assertThrows(AccountOperationException.class, () -> a.deposit(Amount.of("1")));
        assertThrows(AccountOperationException.class, () -> a.withdraw(Amount.of("1")));
        assertThrows(AccountOperationException.class, () -> a.close(NOW));
    }

    @Test
    @DisplayName("Перевірка однаковості валют для переказу")
    void currencyMustMatch() {
        Account uah = account();
        Account usd = Account.open(1L, AccountKind.CURRENT, Currency.USD, 2);
        assertThrows(AccountOperationException.class, () -> uah.requireSameCurrency(usd));
        uah.requireSameCurrency(Account.open(1L, AccountKind.SAVINGS, Currency.UAH, 3));
    }
}

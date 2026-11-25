package com.mini_banking_app.account;

import com.mini_banking_app.account.api.AccountAccessDeniedException;
import com.mini_banking_app.account.api.AccountClosedEvent;
import com.mini_banking_app.account.api.AccountKind;
import com.mini_banking_app.account.api.AccountOperationException;
import com.mini_banking_app.account.api.AccountView;
import com.mini_banking_app.account.api.InsufficientFundsException;
import com.mini_banking_app.account.api.MoneyMovedEvent;
import com.mini_banking_app.account.api.MovementType;
import com.mini_banking_app.account.application.AccountService;
import com.mini_banking_app.shared.domain.Currency;
import com.mini_banking_app.shared.domain.InvalidValueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Сценарії модуля account на тестових адаптерах (сховище в пам'яті, фіктивний довідник користувачів).
 * Кожен тест, позначений «IDOR», відтворює вразливість версії 1.0, знайдену зондом tools/probe.py.
 */
class AccountServiceTest {

    private InMemoryAccountStore store;
    private FakeUserDirectory users;
    private List<Object> events;
    private AccountService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryAccountStore();
        users = new FakeUserDirectory();
        users.add(1, "user", false);
        users.add(2, "admin", true);
        users.add(3, "other", false);
        events = new ArrayList<>();
        Clock clock = Clock.fixed(Instant.parse("2026-12-01T08:00:00Z"), ZoneId.of("Europe/Kyiv"));
        service = new AccountService(store, users, events::add, clock);
    }

    private AccountView open(long ownerId, Currency currency) {
        AccountView view = service.open(ownerId, AccountKind.CURRENT, currency, "admin");
        events.clear();
        return view;
    }

    private BigDecimal balance(Long id) {
        return store.findById(id).orElseThrow().getBalance();
    }

    private static BigDecimal money(String v) {
        return new BigDecimal(v);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, money(expected).compareTo(actual), "очікувалось " + expected + ", отримано " + actual);
    }

    @Test
    @DisplayName("Відкривати рахунки може лише адміністратор")
    void onlyAdminOpens() {
        assertThrows(AccountAccessDeniedException.class,
                () -> service.open(1L, AccountKind.CURRENT, Currency.UAH, "user"));
        AccountView v = service.open(1L, AccountKind.SAVINGS, Currency.USD, "admin");
        assertEquals("Ощадний #1 (USD)", v.label());
        assertEquals(1L, v.ownerId());
    }

    @Test
    @DisplayName("Номери рахунків не повторюються після закриття")
    void sequenceCountsClosedAccounts() {
        AccountView first = open(1, Currency.UAH);
        service.close(first.id(), "test", "user");
        AccountView second = open(1, Currency.UAH);
        assertEquals("Поточний #2 (UAH)", second.label());
    }

    @Test
    @DisplayName("Поповнення публікує подію з балансом після операції")
    void depositPublishesEvent() {
        AccountView acc = open(1, Currency.UAH);
        service.deposit(acc.id(), money("250.00"), "user");
        assertMoney("250.00", balance(acc.id()));
        assertEquals(1, events.size());
        MoneyMovedEvent e = (MoneyMovedEvent) events.get(0);
        assertEquals(MovementType.DEPOSIT, e.type());
        assertEquals(1L, e.actorId());
        assertEquals(Currency.UAH, e.currency());
        assertMoney("250.00", e.balanceAfter());
    }

    @Test
    @DisplayName("IDOR: зняття коштів з чужого рахунку заборонене")
    void cannotWithdrawFromForeignAccount() {
        AccountView foreign = open(3, Currency.UAH);
        service.deposit(foreign.id(), money("500"), "other");
        events.clear();
        assertThrows(AccountAccessDeniedException.class,
                () -> service.withdraw(foreign.id(), money("100"), "user"));
        assertMoney("500", balance(foreign.id()));
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("IDOR: поповнення чужого рахунку заборонене")
    void cannotDepositToForeignAccount() {
        AccountView foreign = open(3, Currency.UAH);
        assertThrows(AccountAccessDeniedException.class,
                () -> service.deposit(foreign.id(), money("1"), "user"));
    }

    @Test
    @DisplayName("Адміністратор не змінює баланс клієнтського рахунку")
    void adminCannotMoveClientMoney() {
        AccountView acc = open(1, Currency.UAH);
        assertThrows(AccountAccessDeniedException.class,
                () -> service.deposit(acc.id(), money("1"), "admin"));
    }

    @Test
    @DisplayName("Зняття понад залишок відхиляється без побічних ефектів")
    void insufficientFunds() {
        AccountView acc = open(1, Currency.UAH);
        service.deposit(acc.id(), money("10"), "user");
        events.clear();
        assertThrows(InsufficientFundsException.class, () -> service.withdraw(acc.id(), money("10.01"), "user"));
        assertMoney("10", balance(acc.id()));
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("Некоректна сума відхиляється до будь-яких змін")
    void invalidAmountRejectedFirst() {
        AccountView acc = open(1, Currency.UAH);
        assertThrows(InvalidValueException.class, () -> service.deposit(acc.id(), money("0.005"), "user"));
        assertThrows(InvalidValueException.class, () -> service.deposit(acc.id(), money("-5"), "user"));
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("Переказ між власними рахунками: дві події зі спільним reference")
    void transferBetweenOwnAccounts() {
        AccountView a = open(1, Currency.UAH);
        AccountView b = open(1, Currency.UAH);
        service.deposit(a.id(), money("300"), "user");
        events.clear();

        service.transfer(a.id(), b.id(), money("120.50"), "user");

        assertMoney("179.50", balance(a.id()));
        assertMoney("120.50", balance(b.id()));
        assertEquals(2, events.size());
        MoneyMovedEvent out = (MoneyMovedEvent) events.get(0);
        MoneyMovedEvent in = (MoneyMovedEvent) events.get(1);
        assertEquals(MovementType.TRANSFER_OUT, out.type());
        assertEquals(MovementType.TRANSFER_IN, in.type());
        assertNotNull(out.reference());
        assertEquals(out.reference(), in.reference());
    }

    @Test
    @DisplayName("Переказ у зворотному напрямку (від більшого id до меншого)")
    void transferReverseOrder() {
        AccountView a = open(1, Currency.UAH);
        AccountView b = open(1, Currency.UAH);
        service.deposit(b.id(), money("50"), "user");
        service.transfer(b.id(), a.id(), money("20"), "user");
        assertMoney("20", balance(a.id()));
        assertMoney("30", balance(b.id()));
    }

    @Test
    @DisplayName("Переказ між валютами, на той самий або чужий рахунок заборонений")
    void transferRules() {
        AccountView uah = open(1, Currency.UAH);
        AccountView usd = open(1, Currency.USD);
        AccountView foreign = open(3, Currency.UAH);
        service.deposit(uah.id(), money("100"), "user");

        assertThrows(AccountOperationException.class, () -> service.transfer(uah.id(), usd.id(), money("1"), "user"));
        assertThrows(AccountOperationException.class, () -> service.transfer(uah.id(), uah.id(), money("1"), "user"));
        assertThrows(AccountAccessDeniedException.class, () -> service.transfer(uah.id(), foreign.id(), money("1"), "user"));
        assertMoney("100", balance(uah.id()));
        assertMoney("0", balance(usd.id()));
        assertMoney("0", balance(foreign.id()));
    }

    @Test
    @DisplayName("Закриття: лише з нульовим балансом, власником або адміністратором")
    void closeRules() {
        AccountView acc = open(1, Currency.UAH);
        service.deposit(acc.id(), money("5"), "user");

        assertThrows(AccountAccessDeniedException.class, () -> service.close(acc.id(), "x", "other"));
        assertThrows(AccountOperationException.class, () -> service.close(acc.id(), "x", "user"));

        service.withdraw(acc.id(), money("5"), "user");
        events.clear();
        service.close(acc.id(), "  Переїзд за кордон  ", "user");

        assertFalse(store.findById(acc.id()).orElseThrow().isActive());
        AccountClosedEvent e = (AccountClosedEvent) events.get(0);
        assertEquals("user", e.closedBy());
        assertEquals("Переїзд за кордон", e.reason());
        assertTrue(service.activeAccountsOf("user").isEmpty());
        assertEquals(1, service.allAccountsOf("user").size());
    }

    @Test
    @DisplayName("Адміністратор може закрити рахунок клієнта з нульовим балансом")
    void adminCloses() {
        AccountView acc = open(1, Currency.EUR);
        service.close(acc.id(), null, "admin");
        assertEquals("Не вказано", ((AccountClosedEvent) events.get(0)).reason());
    }

    @Test
    @DisplayName("Перегляд рахунку: власник і адміністратор — так, інші — ні")
    void viewAccess() {
        AccountView acc = open(1, Currency.UAH);
        assertEquals(acc.id(), service.accountFor(acc.id(), "user").id());
        assertEquals(acc.id(), service.accountFor(acc.id(), "admin").id());
        assertThrows(AccountAccessDeniedException.class, () -> service.accountFor(acc.id(), "other"));
    }

    @Test
    @DisplayName("Моніторинг адміністратора не містить його власних рахунків")
    void monitoringExcludesOwn() {
        open(1, Currency.UAH);
        open(2, Currency.UAH);
        List<AccountView> others = service.activeAccountsExcept("admin");
        assertEquals(1, others.size());
        assertEquals(1L, others.get(0).ownerId());
    }
}

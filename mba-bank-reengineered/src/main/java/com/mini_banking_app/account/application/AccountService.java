package com.mini_banking_app.account.application;

import com.mini_banking_app.account.api.AccountAccessDeniedException;
import com.mini_banking_app.account.api.AccountClosedEvent;
import com.mini_banking_app.account.api.AccountKind;
import com.mini_banking_app.account.api.AccountNotFoundException;
import com.mini_banking_app.account.api.AccountOperationException;
import com.mini_banking_app.account.api.AccountOperations;
import com.mini_banking_app.account.api.AccountQueries;
import com.mini_banking_app.account.api.AccountView;
import com.mini_banking_app.account.api.MoneyMovedEvent;
import com.mini_banking_app.account.api.MovementType;
import com.mini_banking_app.account.domain.Account;
import com.mini_banking_app.shared.domain.Amount;
import com.mini_banking_app.shared.domain.Currency;
import com.mini_banking_app.user.api.UserDirectory;
import com.mini_banking_app.user.api.UserRef;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Прикладний сервіс модуля account: сценарії використання та перевірка прав доступу.
 * <p>
 * Ключові відмінності від першої версії:
 * <ul>
 *   <li>перевірка належності рахунку для всіх операцій (усунено IDOR);</li>
 *   <li>песимістичне блокування рядка рахунку під час зміни балансу (усунено гонку);</li>
 *   <li>журнал операцій ведеться через доменну подію, а не прямим викликом репозиторію іншого модуля.</li>
 * </ul>
 */
@Service
@Transactional
public class AccountService implements AccountOperations, AccountQueries {

    private final AccountStore accounts;
    private final UserDirectory users;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AccountService(AccountStore accounts, UserDirectory users,
                          ApplicationEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.users = users;
        this.events = events;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- команди

    @Override
    public AccountView open(Long ownerId, AccountKind kind, Currency currency, String actorUsername) {
        UserRef actor = users.require(actorUsername);
        if (!actor.admin()) {
            throw new AccountAccessDeniedException();
        }
        UserRef owner = users.requireById(ownerId);
        long sequence = accounts.countByOwner(owner.id()) + 1;
        Account account = accounts.save(Account.open(owner.id(), kind, currency, sequence));
        return toView(account);
    }

    @Override
    public AccountView deposit(Long accountId, BigDecimal value, String actorUsername) {
        Amount amount = Amount.of(value);
        UserRef actor = users.require(actorUsername);
        Account account = lockOwned(accountId, actor);
        account.deposit(amount);
        accounts.save(account);
        publishMovement(account, actor, MovementType.DEPOSIT, amount, null);
        return toView(account);
    }

    @Override
    public AccountView withdraw(Long accountId, BigDecimal value, String actorUsername) {
        Amount amount = Amount.of(value);
        UserRef actor = users.require(actorUsername);
        Account account = lockOwned(accountId, actor);
        account.withdraw(amount);
        accounts.save(account);
        publishMovement(account, actor, MovementType.WITHDRAW, amount, null);
        return toView(account);
    }

    @Override
    public void transfer(Long fromId, Long toId, BigDecimal value, String actorUsername) {
        if (fromId == null || toId == null || fromId.equals(toId)) {
            throw new AccountOperationException("Оберіть два різні рахунки для переказу");
        }
        Amount amount = Amount.of(value);
        UserRef actor = users.require(actorUsername);

        // Блокуємо рахунки у фіксованому порядку (за зростанням id), щоб уникнути взаємоблокувань.
        Long first = Math.min(fromId, toId);
        Long second = Math.max(fromId, toId);
        Account a = lockOwned(first, actor);
        Account b = lockOwned(second, actor);
        Account from = a.getId().equals(fromId) ? a : b;
        Account to = from == a ? b : a;

        from.requireSameCurrency(to);
        from.withdraw(amount);
        to.deposit(amount);
        accounts.save(from);
        accounts.save(to);

        String reference = UUID.randomUUID().toString();
        publishMovement(from, actor, MovementType.TRANSFER_OUT, amount, reference);
        publishMovement(to, actor, MovementType.TRANSFER_IN, amount, reference);
    }

    @Override
    public void close(Long accountId, String reason, String actorUsername) {
        UserRef actor = users.require(actorUsername);
        Account account = accounts.lockById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (!account.isOwnedBy(actor.id()) && !actor.admin()) {
            throw new AccountAccessDeniedException();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        account.close(now);
        accounts.save(account);
        events.publishEvent(new AccountClosedEvent(account.getId(), account.getLabel(),
                account.getOwnerId(), actor.username(), sanitizeReason(reason), now));
    }

    // ---------------------------------------------------------------- запити

    @Override
    @Transactional(readOnly = true)
    public List<AccountView> activeAccountsOf(String username) {
        return views(accounts.findActiveByOwner(users.require(username).id()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountView> allAccountsOf(String username) {
        return views(accounts.findAllByOwner(users.require(username).id()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountView> activeAccountsExcept(String username) {
        return views(accounts.findActiveByOwnerNot(users.require(username).id()));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountView accountFor(Long accountId, String username) {
        UserRef actor = users.require(username);
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (!account.isOwnedBy(actor.id()) && !actor.admin()) {
            throw new AccountAccessDeniedException();
        }
        return toView(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AccountView> byIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return accounts.findAllById(ids).stream()
                .map(AccountService::toView)
                .collect(Collectors.toMap(AccountView::id, Function.identity()));
    }

    // ---------------------------------------------------------------- допоміжне

    /** Операції з коштами дозволені лише власнику рахунку — навіть адміністратор не змінює чужий баланс. */
    private Account lockOwned(Long accountId, UserRef actor) {
        Account account = accounts.lockById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (!account.isOwnedBy(actor.id())) {
            throw new AccountAccessDeniedException();
        }
        return account;
    }

    private void publishMovement(Account account, UserRef actor, MovementType type,
                                 Amount amount, String reference) {
        events.publishEvent(new MoneyMovedEvent(account.getId(), actor.id(), type, amount.value(),
                account.getCurrency(), account.getBalance(), reference, LocalDateTime.now(clock)));
    }

    private static String sanitizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Не вказано";
        }
        String trimmed = reason.strip();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }

    private static List<AccountView> views(List<Account> list) {
        return list.stream().map(AccountService::toView).toList();
    }

    static AccountView toView(Account a) {
        return new AccountView(a.getId(), a.getLabel(), a.getKind(), a.getBalance(),
                a.getCurrency(), a.getOwnerId(), a.isActive());
    }
}

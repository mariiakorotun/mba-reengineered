package com.mini_banking_app.web;

import com.mini_banking_app.account.api.AccountAccessDeniedException;
import com.mini_banking_app.account.api.AccountNotFoundException;
import com.mini_banking_app.account.api.AccountOperations;
import com.mini_banking_app.account.api.AccountQueries;
import com.mini_banking_app.account.api.AccountView;
import com.mini_banking_app.ledger.api.LedgerQueries;
import com.mini_banking_app.user.api.UserDirectory;
import com.mini_banking_app.web.form.AmountForm;
import com.mini_banking_app.web.form.CloseAccountForm;
import com.mini_banking_app.web.form.TransferForm;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Сторінки та операції з рахунками. Контролер не містить бізнес-правил і не звертається
 * до репозиторіїв — лише до контрактів модулів (account.api, ledger.api).
 */
@Controller
public class AccountController {

    private final AccountQueries accountQueries;
    private final AccountOperations accountOperations;
    private final LedgerQueries ledger;
    private final UserDirectory users;

    public AccountController(AccountQueries accountQueries,
                             AccountOperations accountOperations,
                             LedgerQueries ledger,
                             UserDirectory users) {
        this.accountQueries = accountQueries;
        this.accountOperations = accountOperations;
        this.ledger = ledger;
        this.users = users;
    }

    @GetMapping("/accounts")
    public String accounts(Authentication authentication, Model model) {
        model.addAttribute("accounts", accountQueries.activeAccountsOf(authentication.getName()));
        return "accounts";
    }

    /** У першій версії маршрут посилався на неіснуючий шаблон і завжди повертав HTTP 500. */
    @GetMapping("/accounts/{id}")
    public String details(@PathVariable Long id, Authentication authentication, Model model) {
        AccountView account;
        try {
            account = accountQueries.accountFor(id, authentication.getName());
        } catch (AccountNotFoundException e) {
            return "redirect:/accounts?error=not_found";
        } catch (AccountAccessDeniedException e) {
            return "redirect:/accounts?error=access_denied";
        }
        var history = ledger.historyOf(List.of(id)).stream()
                .map(tx -> new TransactionRow(tx, account.label(),
                        tx.currency() != null ? tx.currency() : account.currency(), null))
                .toList();
        model.addAttribute("account", account);
        model.addAttribute("rows", history);
        model.addAttribute("owner", users.require(authentication.getName()).id().equals(account.ownerId()));
        return "account-details";
    }

    @PostMapping("/accounts/{id}/deposit")
    public String deposit(@PathVariable Long id,
                          @Valid @ModelAttribute AmountForm form, BindingResult binding,
                          Authentication authentication, RedirectAttributes flash) {
        Flash.run(flash, binding, "Рахунок успішно поповнено!",
                () -> accountOperations.deposit(id, form.amount(), authentication.getName()));
        return back(form.from(), id);
    }

    @PostMapping("/accounts/{id}/withdraw")
    public String withdraw(@PathVariable Long id,
                           @Valid @ModelAttribute AmountForm form, BindingResult binding,
                           Authentication authentication, RedirectAttributes flash) {
        Flash.run(flash, binding, "Кошти успішно знято!",
                () -> accountOperations.withdraw(id, form.amount(), authentication.getName()));
        return back(form.from(), id);
    }

    @PostMapping("/accounts/transfer")
    public String transfer(@Valid @ModelAttribute TransferForm form, BindingResult binding,
                           Authentication authentication, RedirectAttributes flash) {
        Flash.run(flash, binding, "Переказ виконано!",
                () -> accountOperations.transfer(form.fromAccountId(), form.toAccountId(),
                        form.amount(), authentication.getName()));
        return "redirect:/accounts";
    }

    @PostMapping("/accounts/close")
    public String close(@Valid @ModelAttribute CloseAccountForm form, BindingResult binding,
                        Authentication authentication, RedirectAttributes flash) {
        Flash.run(flash, binding, "Рахунок закрито. Адміністратора повідомлено.",
                () -> accountOperations.close(form.accountId(), form.closureReason(), authentication.getName()));
        return "redirect:/dashboard";
    }

    /** Адміністративне закриття. Раніше рахунок видалявся фізично разом з історією операцій. */
    @PostMapping("/accounts/delete-direct")
    public String closeByAdmin(@RequestParam("accountId") Long accountId,
                               Authentication authentication, RedirectAttributes flash) {
        Flash.run(flash, null, "Рахунок закрито адміністратором.",
                () -> accountOperations.close(accountId, "Закрито адміністратором", authentication.getName()));
        return "redirect:/dashboard";
    }

    /** Повернення лише на внутрішні адреси з білого списку (без Referer). */
    private static String back(String from, Long id) {
        return Map.of("details", "redirect:/accounts/" + id)
                .getOrDefault(from == null ? "" : from, "redirect:/accounts");
    }
}

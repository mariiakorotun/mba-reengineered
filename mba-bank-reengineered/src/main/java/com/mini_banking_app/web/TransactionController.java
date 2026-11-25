package com.mini_banking_app.web;

import com.mini_banking_app.account.api.AccountQueries;
import com.mini_banking_app.account.api.AccountView;
import com.mini_banking_app.ledger.api.LedgerQueries;
import com.mini_banking_app.ledger.api.TransactionView;
import com.mini_banking_app.user.api.UserDirectory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Історія операцій. Дані трьох модулів (ledger, account, user) поєднуються тут,
 * у шарі представлення, пакетними запитами — без N+1 і без JPA-сутностей у шаблонах.
 */
@Controller
public class TransactionController {

    private final LedgerQueries ledger;
    private final AccountQueries accounts;
    private final UserDirectory users;

    public TransactionController(LedgerQueries ledger, AccountQueries accounts, UserDirectory users) {
        this.ledger = ledger;
        this.accounts = accounts;
        this.users = users;
    }

    @GetMapping("/transactions")
    public String transactions(Model model, Authentication authentication) {
        boolean admin = CurrentUser.isAdmin(authentication);
        List<TransactionView> history;
        if (admin) {
            history = ledger.latest();
        } else {
            // Користувач бачить історію всіх своїх рахунків, зокрема закритих.
            List<Long> ids = accounts.allAccountsOf(authentication.getName()).stream()
                    .map(AccountView::id).toList();
            history = ledger.historyOf(ids);
        }
        model.addAttribute("rows", toRows(history, admin));
        return "transactions";
    }

    private List<TransactionRow> toRows(List<TransactionView> history, boolean withActors) {
        Map<Long, AccountView> accountById = accounts.byIds(
                history.stream().map(TransactionView::accountId).collect(Collectors.toSet()));
        Map<Long, String> actorById = withActors
                ? users.usernames(history.stream().map(TransactionView::actorId).collect(Collectors.toSet()))
                : Map.of();
        return history.stream().map(tx -> {
            AccountView acc = accountById.get(tx.accountId());
            return new TransactionRow(tx,
                    acc != null ? acc.label() : "—",
                    tx.currency() != null ? tx.currency() : (acc != null ? acc.currency() : null),
                    actorById.get(tx.actorId()));
        }).toList();
    }
}

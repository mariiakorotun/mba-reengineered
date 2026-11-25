package com.mini_banking_app.web;

import com.mini_banking_app.account.api.AccountQueries;
import com.mini_banking_app.user.api.UserDirectory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final AccountQueries accounts;
    private final UserDirectory users;

    public DashboardController(AccountQueries accounts, UserDirectory users) {
        this.accounts = accounts;
        this.users = users;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("accounts", accounts.activeAccountsOf(username));
        if (CurrentUser.isAdmin(authentication)) {
            model.addAttribute("userId", users.require(username).id());
            // Фільтрація виконується запитом до БД, а не в пам'яті, як у першій версії.
            model.addAttribute("allAccounts", accounts.activeAccountsExcept(username));
        }
        return "dashboard";
    }
}

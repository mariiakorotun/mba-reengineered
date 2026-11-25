package com.mini_banking_app.web;

import com.mini_banking_app.account.api.AccountKind;
import com.mini_banking_app.account.api.AccountOperations;
import com.mini_banking_app.shared.domain.Currency;
import com.mini_banking_app.shared.domain.InvalidValueException;
import com.mini_banking_app.user.api.UserAdministration;
import com.mini_banking_app.web.form.CreateAccountForm;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final UserAdministration userAdministration;
    private final AccountOperations accountOperations;

    public AdminController(UserAdministration userAdministration, AccountOperations accountOperations) {
        this.userAdministration = userAdministration;
        this.accountOperations = accountOperations;
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("users", userAdministration.allUsers());
        model.addAttribute("currencies", Currency.values());
        model.addAttribute("kinds", AccountKind.values());
        return "admin";
    }

    @PostMapping("/admin/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id, Authentication authentication,
                                   RedirectAttributes flash) {
        Flash.run(flash, null, "Статус користувача змінено!",
                () -> userAdministration.toggleEnabled(id, authentication.getName()));
        return "redirect:/admin";
    }

    @PostMapping("/admin/accounts/create")
    public String createAccount(@Valid @ModelAttribute CreateAccountForm form, BindingResult binding,
                                Authentication authentication, RedirectAttributes flash) {
        Flash.run(flash, binding, "Рахунок успішно створено!",
                () -> accountOperations.open(form.userId(), parseKind(form.type()),
                        Currency.parse(form.currency()), authentication.getName()));
        // Замість заголовка Referer (відкритий редирект у першій версії) — фіксований білий список.
        return "dashboard".equals(form.returnTo()) ? "redirect:/dashboard" : "redirect:/admin";
    }

    private static AccountKind parseKind(String type) {
        if (type == null || type.isBlank()) {
            return AccountKind.CURRENT;
        }
        try {
            return AccountKind.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("Непідтримуваний тип рахунку: " + type);
        }
    }
}

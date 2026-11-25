package com.mini_banking_app.web;

import com.mini_banking_app.shared.domain.DomainException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Виконання команди з повідомленням для користувача.
 * Перехоплюються лише порушення бізнес-правил; технічні збої не маскуються
 * (у першій версії catch (Exception e) показував користувачу внутрішні повідомлення).
 */
final class Flash {

    private Flash() {
    }

    static void run(RedirectAttributes flash, BindingResult binding, String success, Runnable command) {
        if (binding != null && binding.hasErrors()) {
            var error = binding.getAllErrors().get(0);
            boolean typeMismatch = error.getCodes() != null
                    && java.util.Arrays.asList(error.getCodes()).contains("typeMismatch");
            flash.addFlashAttribute("errorMessage",
                    typeMismatch ? "Некоректний формат введених даних" : error.getDefaultMessage());
            return;
        }
        try {
            command.run();
            flash.addFlashAttribute("successMessage", success);
        } catch (DomainException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
    }
}

package com.mini_banking_app.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountForm(
        @NotNull(message = "Не вказано користувача") Long userId,
        @NotBlank(message = "Оберіть валюту") String currency,
        String type,
        String returnTo) {
}

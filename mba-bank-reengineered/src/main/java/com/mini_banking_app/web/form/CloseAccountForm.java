package com.mini_banking_app.web.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Поля adminEmail і accountLabel зі старої форми більше не приймаються від клієнта. */
public record CloseAccountForm(
        @NotNull(message = "Не вказано рахунок") Long accountId,
        @Size(max = 200, message = "Причина занадто довга") String closureReason) {
}

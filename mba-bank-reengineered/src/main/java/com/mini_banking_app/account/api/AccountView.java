package com.mini_banking_app.account.api;

import com.mini_banking_app.shared.domain.Currency;

import java.math.BigDecimal;

/** Незмінне представлення рахунку для інших модулів і шаблонів. */
public record AccountView(Long id,
                          String label,
                          AccountKind kind,
                          BigDecimal balance,
                          Currency currency,
                          Long ownerId,
                          boolean active) {
}

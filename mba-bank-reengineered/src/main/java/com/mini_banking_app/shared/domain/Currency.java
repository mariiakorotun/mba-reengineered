package com.mini_banking_app.shared.domain;

import java.util.Arrays;

/** Підтримувані валюти рахунків. Раніше валюта була довільним рядком. */
public enum Currency {
    UAH("Гривня"),
    USD("Долар США"),
    EUR("Євро");

    private final String title;

    Currency(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    public static Currency parse(String code) {
        if (code == null) {
            throw new InvalidValueException("Валюту не вказано");
        }
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidValueException("Непідтримувана валюта: " + code));
    }
}

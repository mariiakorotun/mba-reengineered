package com.mini_banking_app.shared.domain;

import java.math.BigDecimal;

/**
 * Сума фінансової операції (Value Object).
 * <p>
 * Інваріанти: додатна, не більше двох знаків після коми, не перевищує ліміт однієї операції.
 * У першій версії ці перевірки були розпорошені по сервісу або відсутні
 * (наприклад, 0.005 мовчки округлювалося до 0.01).
 */
public record Amount(BigDecimal value) {

    public static final BigDecimal MAX_PER_OPERATION = new BigDecimal("1000000.00");

    public Amount {
        if (value == null) {
            throw new InvalidValueException("Суму не вказано");
        }
        if (value.signum() <= 0) {
            throw new InvalidValueException("Сума повинна бути більшою за нуль");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new InvalidValueException("Сума може містити не більше двох знаків після коми");
        }
        if (value.compareTo(MAX_PER_OPERATION) > 0) {
            throw new InvalidValueException("Сума перевищує ліміт однієї операції (1 000 000.00)");
        }
        value = value.setScale(2);
    }

    public static Amount of(BigDecimal value) {
        return new Amount(value);
    }

    public static Amount of(String value) {
        try {
            return new Amount(new BigDecimal(value));
        } catch (NumberFormatException e) {
            throw new InvalidValueException("Некоректний формат суми");
        }
    }
}

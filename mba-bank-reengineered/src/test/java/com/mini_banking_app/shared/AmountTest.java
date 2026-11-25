package com.mini_banking_app.shared;

import com.mini_banking_app.shared.domain.Amount;
import com.mini_banking_app.shared.domain.Currency;
import com.mini_banking_app.shared.domain.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AmountTest {

    @Test
    @DisplayName("Сума нормалізується до двох знаків після коми")
    void normalizesScale() {
        assertEquals(new BigDecimal("10.50"), Amount.of(new BigDecimal("10.5")).value());
    }

    @Test
    @DisplayName("Нульова та від'ємна суми відхиляються")
    void rejectsNonPositive() {
        assertThrows(InvalidValueException.class, () -> Amount.of(BigDecimal.ZERO));
        assertThrows(InvalidValueException.class, () -> Amount.of(new BigDecimal("-1")));
    }

    @Test
    @DisplayName("Три знаки після коми відхиляються (у версії 1.0 мовчки округлювалися)")
    void rejectsThreeDecimals() {
        assertThrows(InvalidValueException.class, () -> Amount.of(new BigDecimal("0.005")));
    }

    @Test
    @DisplayName("Сума понад ліміт однієї операції відхиляється")
    void rejectsAboveLimit() {
        assertThrows(InvalidValueException.class, () -> Amount.of(new BigDecimal("1000000.01")));
        assertEquals(new BigDecimal("1000000.00"), Amount.of(new BigDecimal("1000000")).value());
    }

    @Test
    @DisplayName("Нечисловий рядок відхиляється зрозумілим повідомленням")
    void rejectsGarbage() {
        InvalidValueException e = assertThrows(InvalidValueException.class, () -> Amount.of("12abc"));
        assertEquals("Некоректний формат суми", e.getMessage());
    }

    @Test
    @DisplayName("Валюта приймається лише з довідника")
    void currencyWhitelist() {
        assertEquals(Currency.USD, Currency.parse("usd"));
        assertThrows(InvalidValueException.class, () -> Currency.parse("BTC"));
        assertThrows(InvalidValueException.class, () -> Currency.parse(null));
    }
}

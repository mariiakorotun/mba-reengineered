package com.mini_banking_app.web.form;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferForm(
        @NotNull(message = "Оберіть рахунок списання") Long fromAccountId,
        @NotNull(message = "Оберіть рахунок зарахування") Long toAccountId,
        @NotNull(message = "Вкажіть суму")
        @DecimalMin(value = "0.01", message = "Сума повинна бути не меншою за 0.01")
        @DecimalMax(value = "1000000.00", message = "Сума перевищує ліміт однієї операції (1 000 000.00)")
        @Digits(integer = 13, fraction = 2, message = "Сума може містити не більше двох знаків після коми")
        BigDecimal amount) {
}

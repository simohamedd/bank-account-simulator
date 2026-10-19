package com.simo.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Money {
    private Money() {
    }

    static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    static String format(BigDecimal value) {
        return (value.signum() < 0 ? "-£" : "£") + round(value.abs()).toPlainString();
    }

    static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be more than zero");
        }
    }
}

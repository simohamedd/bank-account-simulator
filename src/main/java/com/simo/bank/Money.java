package com.simo.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Money {
    private Money() {
    }

    /** Parses an amount typed by a person; rejects text that isn't a number or has fractions of a penny. */
    static BigDecimal of(String value) {
        BigDecimal amount = parse(value, "amount");
        requireWholePennies(amount);
        return round(amount);
    }

    /** Parses a decimal such as an interest rate, giving a readable error instead of NumberFormatException. */
    static BigDecimal parse(String value, String what) {
        String text = value == null ? "" : value.trim();
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' isn't a valid " + what);
        }
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
        requireWholePennies(amount);
    }

    static void requireWholePennies(BigDecimal amount) {
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amounts can't include fractions of a penny: " + amount.toPlainString());
        }
    }
}

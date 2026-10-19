package com.simo.bank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(LocalDateTime at, Type type, BigDecimal amount, BigDecimal balanceAfter, String description) {

    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, INTEREST }

    @Override
    public String toString() {
        String sign = switch (type) {
            case DEPOSIT, TRANSFER_IN, INTEREST -> "+";
            default -> "-";
        };
        return String.format("%-12s %s%-10s balance %-10s %s", type, sign, Money.format(amount),
                Money.format(balanceAfter), description);
    }
}

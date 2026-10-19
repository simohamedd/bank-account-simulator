package com.simo.bank;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String accountNumber, BigDecimal available) {
        super("Account " + accountNumber + " only has " + Money.format(available) + " available");
    }
}

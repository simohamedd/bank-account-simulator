package com.simo.bank;

import java.math.BigDecimal;
import java.time.Clock;

public class CurrentAccount extends Account {

    private final BigDecimal overdraft;

    public CurrentAccount(String number, String owner, BigDecimal overdraft, Clock clock) {
        super(number, owner, clock);
        if (overdraft.signum() < 0) {
            throw new IllegalArgumentException("Overdraft can't be negative");
        }
        this.overdraft = Money.round(overdraft);
    }

    @Override
    protected BigDecimal overdraftLimit() {
        return overdraft;
    }

    @Override
    public String kind() {
        return "Current";
    }
}

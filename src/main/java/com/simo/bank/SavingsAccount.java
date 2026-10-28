package com.simo.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;

public class SavingsAccount extends Account {

    static final BigDecimal MAX_RATE_PERCENT = new BigDecimal("100");

    private final BigDecimal annualRatePercent;

    public SavingsAccount(String number, String owner, BigDecimal annualRatePercent, Clock clock) {
        super(number, owner, clock);
        if (annualRatePercent == null) {
            throw new IllegalArgumentException("Interest rate is required (use 0 for none)");
        }
        if (annualRatePercent.signum() < 0 || annualRatePercent.compareTo(MAX_RATE_PERCENT) > 0) {
            throw new IllegalArgumentException("Interest rate must be between 0% and " + MAX_RATE_PERCENT + "%");
        }
        this.annualRatePercent = annualRatePercent;
    }

    @Override
    protected BigDecimal overdraftLimit() {
        return BigDecimal.ZERO;
    }

    @Override
    public String kind() {
        return "Savings";
    }

    /** Adds one month of interest (annual rate / 12) and returns the amount added. */
    public BigDecimal applyMonthlyInterest() {
        BigDecimal interest = balance()
                .multiply(annualRatePercent)
                .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_EVEN);
        if (interest.signum() > 0) {
            credit(Transaction.Type.INTEREST, interest, "Monthly interest at " + annualRatePercent.stripTrailingZeros().toPlainString() + "%");
        }
        return interest;
    }

    public BigDecimal annualRatePercent() {
        return annualRatePercent;
    }
}

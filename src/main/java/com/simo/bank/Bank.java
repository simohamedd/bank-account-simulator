package com.simo.bank;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class Bank {

    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final Clock clock;
    private int nextNumber = 10000001;

    public Bank(Clock clock) {
        this.clock = clock;
    }

    public CurrentAccount openCurrent(String owner, BigDecimal overdraft) {
        return register(new CurrentAccount(nextNumber(), owner, overdraft, clock));
    }

    public SavingsAccount openSavings(String owner, BigDecimal annualRatePercent) {
        return register(new SavingsAccount(nextNumber(), owner, annualRatePercent, clock));
    }

    public Account find(String number) {
        Account a = accounts.get(number);
        if (a == null) {
            throw new NoSuchElementException("No account " + number);
        }
        return a;
    }

    public List<Account> accounts() {
        return new ArrayList<>(accounts.values());
    }

    /** Moves money between two accounts; nothing changes if the source can't cover it or either account is closed. */
    public void transfer(String from, String to, BigDecimal amount) {
        Money.requirePositive(amount);
        if (from.equals(to)) {
            throw new IllegalArgumentException("Choose two different accounts");
        }
        Account source = find(from);
        Account target = find(to);
        // Check both ends before touching either balance, so a closed target can't swallow the debit.
        source.requireOpen();
        target.requireOpen();
        source.debit(Transaction.Type.TRANSFER_OUT, amount, "To " + to);
        target.credit(Transaction.Type.TRANSFER_IN, amount, "From " + from);
    }

    /** Applies a month of interest to every savings account and returns the total paid. */
    public BigDecimal endOfMonth() {
        BigDecimal total = BigDecimal.ZERO;
        for (Account a : accounts.values()) {
            if (a instanceof SavingsAccount s && !s.isClosed()) {
                total = total.add(s.applyMonthlyInterest());
            }
        }
        return total;
    }

    /** Closes an account whose balance is zero; see {@link Account#close()}. */
    public Account close(String number) {
        Account a = find(number);
        a.close();
        return a;
    }

    public BigDecimal totalDeposits() {
        return accounts.values().stream().map(Account::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String nextNumber() {
        return String.valueOf(nextNumber++);
    }

    private <T extends Account> T register(T account) {
        accounts.put(account.number(), account);
        return account;
    }
}

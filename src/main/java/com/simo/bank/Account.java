package com.simo.bank;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Account {

    private final String number;
    private final String owner;
    private BigDecimal balance = Money.of("0");
    private final List<Transaction> history = new ArrayList<>();
    private final Clock clock;
    private boolean closed;

    protected Account(String number, String owner, Clock clock) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner name is required");
        }
        this.number = number;
        this.owner = owner.trim();
        this.clock = clock;
    }

    /** How far below zero this account may go. */
    protected abstract BigDecimal overdraftLimit();

    public abstract String kind();

    public BigDecimal available() {
        return balance.add(overdraftLimit());
    }

    public void deposit(BigDecimal amount, String description) {
        Money.requirePositive(amount);
        credit(Transaction.Type.DEPOSIT, amount, description);
    }

    public void withdraw(BigDecimal amount, String description) {
        Money.requirePositive(amount);
        debit(Transaction.Type.WITHDRAWAL, amount, description);
    }

    void credit(Transaction.Type type, BigDecimal amount, String description) {
        requireOpen();
        balance = Money.round(balance.add(amount));
        record(type, amount, description);
    }

    void debit(Transaction.Type type, BigDecimal amount, String description) {
        requireOpen();
        if (amount.compareTo(available()) > 0) {
            throw new InsufficientFundsException(number, available());
        }
        balance = Money.round(balance.subtract(amount));
        record(type, amount, description);
    }

    /**
     * Closes the account. Only an account with a balance of exactly zero can be closed, so no money
     * is lost; afterwards it refuses every movement but keeps its history for statements.
     */
    public void close() {
        requireOpen();
        if (balance.signum() != 0) {
            throw new IllegalStateException("Account " + number + " still holds " + Money.format(balance)
                    + "; bring the balance to zero before closing it");
        }
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    /** Throws if the account has been closed. */
    void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Account " + number + " is closed");
        }
    }

    private void record(Transaction.Type type, BigDecimal amount, String description) {
        history.add(new Transaction(LocalDateTime.now(clock), type, Money.round(amount), balance, description));
    }

    public String number() { return number; }
    public String owner() { return owner; }
    public BigDecimal balance() { return balance; }
    public List<Transaction> history() { return Collections.unmodifiableList(history); }

    @Override
    public String toString() {
        String line = String.format("%s  %-8s %-20s %s", number, kind(), owner, Money.format(balance));
        return closed ? line + "  (closed)" : line;
    }
}

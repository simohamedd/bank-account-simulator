package com.simo.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankTest {

    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank(Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC));
    }

    private static BigDecimal m(String s) {
        return Money.of(s);
    }

    @Test
    void depositsAndWithdrawalsUpdateBalanceAndHistory() {
        CurrentAccount a = bank.openCurrent("Amina", m("0"));
        a.deposit(m("100"), "Salary");
        a.withdraw(m("30.50"), "Groceries");
        assertEquals(m("69.50"), a.balance());
        assertEquals(2, a.history().size());
        assertEquals(Transaction.Type.WITHDRAWAL, a.history().get(1).type());
    }

    @Test
    void currentAccountCanUseOverdraftButNotBeyondIt() {
        CurrentAccount a = bank.openCurrent("Yusuf", m("200"));
        a.withdraw(m("150"), "Rent top-up");
        assertEquals(m("-150.00"), a.balance());
        assertThrows(InsufficientFundsException.class, () -> a.withdraw(m("60"), "Too much"));
        assertEquals(m("-150.00"), a.balance());
    }

    @Test
    void savingsAccountCannotGoOverdrawn() {
        SavingsAccount s = bank.openSavings("Sara", m("4"));
        s.deposit(m("50"), "Start");
        assertThrows(InsufficientFundsException.class, () -> s.withdraw(m("50.01"), "Too much"));
    }

    @Test
    void transferMovesMoneyAndFailsAtomically() {
        CurrentAccount from = bank.openCurrent("Amina", m("0"));
        SavingsAccount to = bank.openSavings("Amina", m("3"));
        from.deposit(m("80"), "Start");

        bank.transfer(from.number(), to.number(), m("30"));
        assertEquals(m("50.00"), from.balance());
        assertEquals(m("30.00"), to.balance());

        assertThrows(InsufficientFundsException.class, () -> bank.transfer(from.number(), to.number(), m("500")));
        assertEquals(m("50.00"), from.balance());
        assertEquals(m("30.00"), to.balance());
    }

    @Test
    void monthlyInterestIsAnnualRateOverTwelve() {
        SavingsAccount s = bank.openSavings("Sara", m("6"));
        s.deposit(m("1000"), "Start");
        assertEquals(m("5.00"), bank.endOfMonth());
        assertEquals(m("1005.00"), s.balance());
    }

    @Test
    void rejectsZeroOrNegativeAmounts() {
        CurrentAccount a = bank.openCurrent("Amina", m("0"));
        assertThrows(IllegalArgumentException.class, () -> a.deposit(m("0"), "Nothing"));
        assertThrows(IllegalArgumentException.class, () -> a.withdraw(m("-5"), "Negative"));
    }

    @Test
    void exportsStatementAsCsv() {
        CurrentAccount a = bank.openCurrent("Amina", m("0"));
        a.deposit(m("100"), "Salary");
        a.withdraw(m("12.50"), "Coffee, cake");
        String[] lines = StatementExporter.toCsv(a).split("\n");
        assertEquals("date,type,description,money_in,money_out,balance", lines[0]);
        assertEquals("2026-10-18 10:00,DEPOSIT,Salary,100.00,,100.00", lines[1]);
        assertEquals("2026-10-18 10:00,WITHDRAWAL,\"Coffee, cake\",,12.50,87.50", lines[2]);
    }

    @Test
    void rejectsFractionsOfAPennyInsteadOfSilentlyRounding() {
        CurrentAccount a = bank.openCurrent("Amina", m("0"));
        assertThrows(IllegalArgumentException.class, () -> a.deposit(new BigDecimal("0.004"), "Dust"));
        assertThrows(IllegalArgumentException.class, () -> a.deposit(new BigDecimal("10.005"), "Odd"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("19.999"));
        assertEquals(0, a.history().size());
        a.deposit(new BigDecimal("10.500"), "Trailing zeros are fine");
        assertEquals(m("10.50"), a.balance());
    }

    @Test
    void parsingNonNumericInputGivesReadableError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Money.of("ten"));
        assertEquals("'ten' isn't a valid amount", e.getMessage());
        assertEquals(m("12.30"), Money.of(" 12.3 "));
    }

    @Test
    void savingsRateMustBeBetweenZeroAndOneHundredPercent() {
        assertThrows(IllegalArgumentException.class, () -> bank.openSavings("Sara", new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> bank.openSavings("Sara", new BigDecimal("100.01")));
        assertThrows(IllegalArgumentException.class, () -> bank.openSavings("Sara", null));
        assertThrows(IllegalArgumentException.class, () -> bank.openCurrent("Sara", null));
        assertEquals(0, bank.accounts().size());
    }

    @Test
    void savingsRateKeepsFullPrecision() {
        SavingsAccount s = bank.openSavings("Sara", Money.parse("4.125", "interest rate"));
        s.deposit(m("1200"), "Start");
        assertEquals(new BigDecimal("4.125"), s.annualRatePercent());
        assertEquals(m("4.12"), bank.endOfMonth());
    }
}

package com.simo.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void onlyAZeroBalanceAccountCanBeClosed() {
        CurrentAccount a = bank.openCurrent("Amina", m("100"));
        a.deposit(m("20"), "Start");
        assertThrows(IllegalStateException.class, () -> bank.close(a.number()));
        a.withdraw(m("50"), "Overdrawn");
        assertThrows(IllegalStateException.class, () -> bank.close(a.number()));
        a.deposit(m("30"), "Clear overdraft");

        bank.close(a.number());
        assertTrue(a.isClosed());
        assertTrue(a.toString().endsWith("(closed)"));
        assertThrows(IllegalStateException.class, () -> bank.close(a.number()));
    }

    @Test
    void closedAccountRefusesEveryMovementButKeepsItsStatement() {
        CurrentAccount a = bank.openCurrent("Amina", m("0"));
        a.deposit(m("10"), "Start");
        a.withdraw(m("10"), "Empty");
        a.close();

        assertThrows(IllegalStateException.class, () -> a.deposit(m("5"), "Late"));
        assertThrows(IllegalStateException.class, () -> a.withdraw(m("5"), "Late"));
        assertEquals(2, a.history().size());
        assertEquals(3, StatementExporter.toCsv(a).split("\n").length);
    }

    @Test
    void transferIntoClosedAccountLeavesSourceUntouched() {
        CurrentAccount from = bank.openCurrent("Amina", m("0"));
        CurrentAccount closed = bank.openCurrent("Yusuf", m("0"));
        from.deposit(m("80"), "Start");
        closed.close();

        assertThrows(IllegalStateException.class, () -> bank.transfer(from.number(), closed.number(), m("30")));
        assertEquals(m("80.00"), from.balance());
        assertEquals(1, from.history().size());
        assertThrows(IllegalStateException.class, () -> bank.transfer(closed.number(), from.number(), m("1")));
    }

    @Test
    void endOfMonthSkipsClosedSavingsAccounts() {
        SavingsAccount open = bank.openSavings("Sara", m("6"));
        SavingsAccount closed = bank.openSavings("Sara", m("6"));
        open.deposit(m("1000"), "Start");
        closed.close();

        assertEquals(m("5.00"), bank.endOfMonth());
        assertEquals(0, closed.history().size());
    }
}

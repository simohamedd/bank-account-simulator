package com.simo.bank;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Scanner;

/** Interactive console for the bank. Run with: mvn -q compile exec:java */
public class BankApp {

    private final Bank bank = new Bank(Clock.systemDefaultZone());
    private final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        new BankApp().run();
    }

    void run() {
        System.out.println("Bank account simulator");
        while (true) {
            System.out.println("""

                    1  Open current account      5  Transfer
                    2  Open savings account      6  Statement
                    3  Deposit                   7  List accounts
                    4  Withdraw                  8  End of month (pay interest)
                    0  Quit""");
            String choice = ask("Choose");
            try {
                switch (choice) {
                    case "1" -> System.out.println("Opened " + bank.openCurrent(ask("Owner name"), money("Overdraft limit")));
                    case "2" -> System.out.println("Opened " + bank.openSavings(ask("Owner name"), money("Annual interest rate %")));
                    case "3" -> { Account a = account(); a.deposit(money("Amount"), "Cash deposit"); System.out.println(a); }
                    case "4" -> { Account a = account(); a.withdraw(money("Amount"), "Cash withdrawal"); System.out.println(a); }
                    case "5" -> { bank.transfer(ask("From account"), ask("To account"), money("Amount")); System.out.println("Transfer complete"); }
                    case "6" -> { Account a = account(); System.out.println(a); a.history().forEach(t -> System.out.println("  " + t)); }
                    case "7" -> { bank.accounts().forEach(System.out::println); System.out.println("Total held: " + Money.format(bank.totalDeposits())); }
                    case "8" -> System.out.println("Interest paid: " + Money.format(bank.endOfMonth()));
                    case "0" -> { System.out.println("Goodbye"); return; }
                    default -> System.out.println("Pick a number from the menu");
                }
            } catch (RuntimeException e) {
                System.out.println("Couldn't do that: " + e.getMessage());
            }
        }
    }

    private Account account() {
        return bank.find(ask("Account number"));
    }

    private BigDecimal money(String prompt) {
        return Money.of(ask(prompt).replace("£", "").replace(",", ""));
    }

    private String ask(String prompt) {
        System.out.print(prompt + ": ");
        if (!in.hasNextLine()) {
            System.exit(0);
        }
        return in.nextLine().trim();
    }
}

# Bank Account Simulator

[![Build and test](https://github.com/simohamedd/bank-account-simulator/actions/workflows/build.yml/badge.svg)](https://github.com/simohamedd/bank-account-simulator/actions/workflows/build.yml)

A console banking app that models current and savings accounts the way a real ledger does: every movement is recorded as a transaction, money uses `BigDecimal`, and failed transfers leave both accounts untouched.

## Features
- Current accounts with an overdraft limit
- Savings accounts that can't go overdrawn and earn monthly interest
- Transfers between accounts, with all-or-nothing behaviour
- Statements showing each transaction and the running balance

## Design
- `Account` is abstract; `CurrentAccount` and `SavingsAccount` differ only in overdraft rules and interest
- A `Clock` is injected so tests are deterministic
- `InsufficientFundsException` is raised before any balance changes

## Run
```bash
mvn -q compile exec:java
```

## Test
```bash
mvn test
```

## Licence
MIT. See [LICENSE](LICENSE).

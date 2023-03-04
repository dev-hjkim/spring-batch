package com.example.batch.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Account {
    private final long id;
    private final BigDecimal balance;
    private final Date lastStatementDate;
    private final List<Transaction2> transactions = new ArrayList<>();

    public Account(long id, BigDecimal balance, Date lastStatementDate) {
        this.id = id;
        this.balance = balance;
        this.lastStatementDate = lastStatementDate;
    }

    public long getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Date getLastStatementDate() {
        return lastStatementDate;
    }

    public List<Transaction2> getTransactions() {
        return transactions;
    }

    public void addTransaction(Transaction2 transaction) {
        this.transactions.add(transaction);
    }
}

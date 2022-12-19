package com.example.batch.domain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Transaction {
    private String accountNumber;
    private Date timestamp;
    private Double amount;

    private DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");

    @Override
    public String toString() {
        return "Transaction{" +
                "accountNumber='" + accountNumber + '\'' +
                ", transactionDate=" + timestamp +
                ", amount=" + amount +
                '}';
    }

    public String getAccountNumber() {
        return this.accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Double getAmount() {
        return this.amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}

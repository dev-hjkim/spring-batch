package com.example.batch.dao;

import com.example.batch.domain.Transaction;

import java.util.List;

public interface TransactionDao {
    List<Transaction> getTransactionsByAccountNumber(String accountNumber);
}

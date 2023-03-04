package com.example.batch.domain;

import java.util.ArrayList;
import java.util.List;

public class Statement {
    private final AppCustomer customer;
    private List<Account> accounts = new ArrayList<>();

    public Statement(AppCustomer customer) {
        this.customer = customer;
    }

    public Statement(AppCustomer customer, List<Account> accounts) {
        this.customer = customer;
        this.accounts.addAll(accounts);
    }

    public AppCustomer getCustomer() {
        return customer;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }
}

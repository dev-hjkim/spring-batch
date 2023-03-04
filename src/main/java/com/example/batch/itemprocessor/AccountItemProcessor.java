package com.example.batch.itemprocessor;

import com.example.batch.domain.Statement;
import com.example.batch.resultSetExtractor.AccountResultSetExtractor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccountItemProcessor implements ItemProcessor<Statement, Statement> {
    @Autowired
    private final JdbcTemplate jdbcTemplate;

    public AccountItemProcessor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Statement process(Statement item) throws Exception {
        item.setAccounts(this.jdbcTemplate.query("SELECT a.id, " +
                "a.balance, " +
                "a.last_statement_date, " +
                "t.transaction_id, " +
                "t.descriptions, " +
                "t.credit, " +
                "t.debit, " +
                "t.timestamp " +
                "FROM account a left join " +
                "transaction t on a.id = t.account_id " +
                "WHERE a.id in " +
                "(SELECT account_id " +
                "FROM app_customer " +
                "WHERE customer_id = ?) " +
                "ORDER BY t.timestamp",
                new Object[] {item.getCustomer().getCustomerId()},
                new AccountResultSetExtractor()));
        return item;
    }
}

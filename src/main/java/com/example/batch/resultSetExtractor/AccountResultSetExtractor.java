package com.example.batch.resultSetExtractor;

import com.example.batch.domain.Account;
import com.example.batch.domain.Transaction2;
import org.apache.shiro.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AccountResultSetExtractor implements ResultSetExtractor<List<Account>> {
    private List<Account> accounts = new ArrayList<>();
    private Account curAccount;

    @Nullable
    @Override
    public List<Account> extractData(ResultSet rs) throws SQLException, DataAccessException {
        while (rs.next()) {
            if (curAccount == null) {
                curAccount = new Account(
                        rs.getLong("id"),
                        rs.getBigDecimal("balance"),
                        rs.getDate("last_statement_date"));
            }
            else if (rs.getLong("id") != curAccount.getId()) {
                accounts.add(curAccount);

                curAccount = new Account(rs.getLong("id"),
                        rs.getBigDecimal("balance"),
                        rs.getDate("last_statement_date"));
            }

            if (StringUtils.hasText(rs.getString("description"))) {
                curAccount.addTransaction(
                        new Transaction2(rs.getLong("transaction_id"),
                                rs.getLong("account_id"),
                                rs.getString("description"),
                                rs.getBigDecimal("credit"),
                                rs.getBigDecimal("debit"),
                                new Date(rs.getTimestamp("timestamp").getTime()))
                );
            }
        }

        if (curAccount != null) {
            accounts.add(curAccount);
        }

        return accounts;
    }
}

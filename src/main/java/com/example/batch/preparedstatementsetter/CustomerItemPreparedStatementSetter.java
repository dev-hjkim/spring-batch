package com.example.batch.preparedstatementsetter;

import com.example.batch.domain.Customer2;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CustomerItemPreparedStatementSetter implements ItemPreparedStatementSetter<Customer2> {
    public void setValues(Customer2 customer2, PreparedStatement ps) throws SQLException {
        ps.setString(1, customer2.getFirstName());
        ps.setString(2, customer2.getMiddleInitial());
        ps.setString(3, customer2.getLastName());
        ps.setString(4, customer2.getAddress());
        ps.setString(5, customer2.getCity());
        ps.setString(6, customer2.getState());
        ps.setString(7, customer2.getZip());
    }
}

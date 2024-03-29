package com.example.batch.domain;

import org.apache.shiro.util.StringUtils;

public class CustomerNameUpdate extends CustomerUpdate {
    private final String firstName;
    private final String middleName;
    private final String lastName;

    public CustomerNameUpdate(long customerId, String firstName,
                              String middleName, String lastName) {
        super(customerId);
        this.firstName = StringUtils.hasText(firstName) ? firstName : null;
        this.middleName = StringUtils.hasText(middleName) ? middleName : null;
        this.lastName = StringUtils.hasText(lastName) ? lastName : null;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }
}

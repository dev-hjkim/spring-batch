package com.example.batch.service;

import com.example.batch.domain.Customer2;
import org.springframework.stereotype.Service;

@Service
public class UpperCaseNameService {
    public Customer2 upperCase(Customer2 customer) {
        Customer2 newCustomer = new Customer2(customer);

        newCustomer.setFirstName(newCustomer.getFirstName().toUpperCase());
        newCustomer.setMiddleInitial(newCustomer.getMiddleInitial().toUpperCase());
        newCustomer.setLastName(newCustomer.getLastName().toUpperCase());

        return newCustomer;
    }
}

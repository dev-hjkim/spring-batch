package com.example.batch.service;

import com.example.batch.domain.JpaCustomer;
import org.springframework.stereotype.Service;

@Service
public class LoggingService {
    public void logCustomer(JpaCustomer customer) {
        System.out.println("I just saved " + customer);
    }

    public void logCustomerAddress(String address,
                                   String city,
                                   String state,
                                   String zip) {
        System.out.println(
                String.format("I just saved the address:\n%s\n%s, %s\n%s",
                        address,
                        city,
                        state,
                        zip)
        );
    }
}

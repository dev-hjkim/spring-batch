package com.example.batch.service;

import com.example.batch.domain.JpaCustomer;
import org.springframework.stereotype.Service;

@Service
public class LoggingService {
    public void logCustomer(JpaCustomer customer) {
        System.out.println("I just saved " + customer);
    }
}

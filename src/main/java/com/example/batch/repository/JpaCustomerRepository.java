package com.example.batch.repository;

import com.example.batch.domain.JpaCustomer;
import org.springframework.data.repository.CrudRepository;

public interface JpaCustomerRepository extends CrudRepository<JpaCustomer, Long> {
}

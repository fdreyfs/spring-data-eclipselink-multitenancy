package com.fdreyfs.tenancy.demo.domain.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.fdreyfs.tenancy.demo.domain.model.Customer;

public interface CustomerRepository extends CrudRepository<Customer, Long>{
    List<Customer> findByLastName(String lastName);
}

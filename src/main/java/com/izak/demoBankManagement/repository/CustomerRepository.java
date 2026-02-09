package com.izak.demoBankManagement.repository;

import com.izak.demoBankManagement.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerId(String customerId);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByUserId(Long userId);
    List<Customer> findByStatus(Customer.Status status);
    List<Customer> findByKycStatus(Customer.KycStatus kycStatus);
    boolean existsByCustomerId(String customerId);
    boolean existsByEmail(String email);
}

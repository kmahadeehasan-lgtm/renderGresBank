package com.izak.demoBankManagement.repository;

import com.izak.demoBankManagement.entity.Account;
import com.izak.demoBankManagement.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    // Pessimistic lock for concurrent transaction safety
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    List<Account> findByCustomer(Customer customer);
    List<Account> findByCustomerId(String customerId);
    List<Account> findByStatus(Account.Status status);
    boolean existsByAccountNumber(String accountNumber);

    @Query("SELECT a FROM Account a WHERE a.customer.customerId = :customerId AND a.status = :status")
    List<Account> findActiveAccountsByCustomerId(@Param("customerId") String customerId, @Param("status") Account.Status status);


    // Branch-scoped query
    List<Account> findByBranchId(Long branchId);
}

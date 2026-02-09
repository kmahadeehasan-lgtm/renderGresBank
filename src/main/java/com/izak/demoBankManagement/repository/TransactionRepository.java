package com.izak.demoBankManagement.repository;

import com.izak.demoBankManagement.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    List<Transaction> findByFromAccountId(Long accountId);
    List<Transaction> findByToAccountId(Long accountId);
    List<Transaction> findByStatus(Transaction.Status status);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.accountNumber = :accountNumber OR t.toAccount.accountNumber = :accountNumber ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("SELECT t FROM Transaction t WHERE (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId) AND t.timestamp BETWEEN :startDate AND :endDate ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountIdAndDateRange(@Param("accountId") Long accountId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.customer.customerId = :customerId OR t.toAccount.customer.customerId = :customerId ORDER BY t.timestamp DESC")
    List<Transaction> findByCustomerId(@Param("customerId") String customerId);

    boolean existsByTransactionId(String transactionId);


    // Find transactions by branch (where either from or to account belongs to the branch)
    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.fromAccount.branch.id = :branchId OR t.toAccount.branch.id = :branchId) " +
            "ORDER BY t.timestamp DESC")
    List<Transaction> findByBranchId(@Param("branchId") Long branchId);

    // Find transactions by branch within date range
    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.fromAccount.branch.id = :branchId OR t.toAccount.branch.id = :branchId) " +
            "AND t.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY t.timestamp DESC")
    List<Transaction> findByBranchIdAndDateRange(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}

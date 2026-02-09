package com.izak.demoBankManagement.repository;

import com.izak.demoBankManagement.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanId(String loanId);

    // FIXED: Changed to use JPQL instead of native query for better type handling
    @Query("SELECT l FROM Loan l WHERE l.customer.customerId = :customerId")
    List<Loan> findByCustomerId(@Param("customerId") String customerId);

    /**
     * Find all loans by customer's customerId (navigates through customer relationship)
     */
    @Query("SELECT l FROM Loan l WHERE l.customer.customerId = :customerId")
    List<Loan> findByCustomerCustomerId(@Param("customerId") String customerId);

    @Query("SELECT l FROM Loan l WHERE l.customer.customerId = :customerId AND l.loanStatus = :status")
    List<Loan> findByCustomerIdAndStatus(@Param("customerId") String customerId,
                                         @Param("status") Loan.LoanStatus status);

    List<Loan> findByAccountId(Long accountId);

    List<Loan> findByLoanStatus(Loan.LoanStatus status);

    List<Loan> findByApprovalStatus(Loan.ApprovalStatus status);

    List<Loan> findByLoanType(Loan.LoanType loanType);

    @Query("SELECT l FROM Loan l WHERE l.loanStatus = :status " +
            "AND EXISTS (SELECT s FROM LoanRepaymentSchedule s WHERE s.loan.id = l.id " +
            "AND s.dueDate < :date AND s.status = 'PENDING')")
    List<Loan> findOverdueLoans(@Param("date") LocalDate date,
                                @Param("status") Loan.LoanStatus status);

    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l " +
            "WHERE l.customer.customerId = :customerId AND l.loanStatus IN ('ACTIVE', 'APPROVED')")
    BigDecimal sumOutstandingByCustomerId(@Param("customerId") String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Loan l WHERE l.loanId = :loanId")
    Optional<Loan> findByLoanIdWithLock(@Param("loanId") String loanId);

    boolean existsByLoanId(String loanId);

    // Pagination support
    Page<Loan> findByLoanStatus(Loan.LoanStatus status, Pageable pageable);

    // FIXED: Changed to use JPQL
    @Query("SELECT l FROM Loan l WHERE l.customer.customerId = :customerId")
    Page<Loan> findByCustomerId(@Param("customerId") String customerId, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.customer.customerId = :customerId " +
            "AND (:status IS NULL OR l.loanStatus = :status) " +
            "AND (:loanType IS NULL OR l.loanType = :loanType)")
    Page<Loan> searchLoans(@Param("customerId") String customerId,
                           @Param("status") Loan.LoanStatus status,
                           @Param("loanType") Loan.LoanType loanType,
                           Pageable pageable);

    // Branch-scoped queries
    @Query("SELECT l FROM Loan l WHERE l.account.branch.id = :branchId")
    List<Loan> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT l FROM Loan l WHERE l.account.branch.id = :branchId")
    Page<Loan> findByBranchId(@Param("branchId") Long branchId, Pageable pageable);
}

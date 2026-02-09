package com.izak.demoBankManagement.repository;

import com.izak.demoBankManagement.entity.Account;
import com.izak.demoBankManagement.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByBranchCode(String branchCode);
    Optional<Branch> findByIfscCode(String ifscCode);
    List<Branch> findByStatus(Branch.BranchStatus status);
    List<Branch> findByCity(String city);
    List<Branch> findByState(String state);
    boolean existsByBranchCode(String branchCode);
    boolean existsByIfscCode(String ifscCode);

    // ============================================
    // NEW METHODS FOR STATISTICS
    // ============================================

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.branch.id = :branchId AND a.status = 'ACTIVE'")
    BigDecimal getTotalBalanceByBranch(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.branch.id = :branchId AND a.status = 'ACTIVE'")
    Long getActiveAccountCountByBranch(@Param("branchId") Long branchId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.status = 'ACTIVE'")
    BigDecimal getTotalBankBalance();

    @Query("SELECT COUNT(a) FROM Account a WHERE a.status = 'ACTIVE'")
    Long getTotalActiveAccounts();

    // Get accounts by branch
    @Query("SELECT a FROM Account a WHERE a.branch.id = :branchId")
    List<Account> getAccountsByBranch(@Param("branchId") Long branchId);

    // Inter-branch transfer statistics
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.fromAccount.branch.id = :branchId OR t.toAccount.branch.id = :branchId")
    Long getTransactionCountByBranch(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.fromAccount.branch.id = :fromBranchId AND t.toAccount.branch.id = :toBranchId")
    Long getInterBranchTransferCount(@Param("fromBranchId") Long fromBranchId, @Param("toBranchId") Long toBranchId);
}

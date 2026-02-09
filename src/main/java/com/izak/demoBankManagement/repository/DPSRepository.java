package com.izak.demoBankManagement.repository;

import com.izak.demoBankManagement.entity.DPS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DPSRepository extends JpaRepository<DPS, Long> {

    // Existing methods
    Optional<DPS> findByDpsNumber(String dpsNumber);
    List<DPS> findByCustomerId(String customerId);
    List<DPS> findByStatus(DPS.DPSStatus status);
    List<DPS> findByBranchId(Long branchId);
    boolean existsByDpsNumber(String dpsNumber);

    // Part A: New branch-scoped query methods

    /**
     * Find DPS accounts by branch and status
     */
    @Query("SELECT d FROM DPS d WHERE d.branch.id = :branchId AND d.status = :status")
    List<DPS> findByBranchIdAndStatus(
            @Param("branchId") Long branchId,
            @Param("status") DPS.DPSStatus status
    );

    /**
     * Find DPS accounts by branch and customer
     */
    @Query("SELECT d FROM DPS d WHERE d.branch.id = :branchId AND d.customerId = :customerId")
    List<DPS> findByBranchIdAndCustomerId(
            @Param("branchId") Long branchId,
            @Param("customerId") String customerId
    );

    /**
     * Count active DPS accounts by branch
     */
    @Query("SELECT COUNT(d) FROM DPS d WHERE d.branch.id = :branchId AND d.status = 'ACTIVE'")
    Long countActiveByBranch(@Param("branchId") Long branchId);

    /**
     * Get total deposited amount by branch
     */
    @Query("SELECT COALESCE(SUM(d.totalDeposited), 0) FROM DPS d WHERE d.branch.id = :branchId AND d.status = 'ACTIVE'")
    BigDecimal getTotalDepositedByBranch(@Param("branchId") Long branchId);
}

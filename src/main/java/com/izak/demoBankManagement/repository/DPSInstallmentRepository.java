package com.izak.demoBankManagement.repository;

import com.izak.demoBankManagement.entity.DPSInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DPSInstallmentRepository extends JpaRepository<DPSInstallment, Long> {

    // Existing methods
    List<DPSInstallment> findByDpsId(Long dpsId);
    List<DPSInstallment> findByStatus(DPSInstallment.InstallmentStatus status);

    @Query("SELECT i FROM DPSInstallment i WHERE i.dps.id = :dpsId AND i.status = :status")
    List<DPSInstallment> findByDpsIdAndStatus(
            @Param("dpsId") Long dpsId,
            @Param("status") DPSInstallment.InstallmentStatus status
    );

    @Query("SELECT i FROM DPSInstallment i WHERE i.dueDate <= :date AND i.status = 'PENDING'")
    List<DPSInstallment> findOverdueInstallments(@Param("date") LocalDate date);

    // Part B: New branch-scoped query methods

    /**
     * Find overdue installments by branch
     */
    @Query("SELECT i FROM DPSInstallment i WHERE i.dps.branch.id = :branchId " +
            "AND i.dueDate < :date AND i.status = 'PENDING'")
    List<DPSInstallment> findOverdueInstallmentsByBranch(
            @Param("branchId") Long branchId,
            @Param("date") LocalDate date
    );

    /**
     * Count pending installments by branch
     */
    @Query("SELECT COUNT(i) FROM DPSInstallment i WHERE i.dps.branch.id = :branchId " +
            "AND i.status = 'PENDING'")
    Long countPendingInstallmentsByBranch(@Param("branchId") Long branchId);
}

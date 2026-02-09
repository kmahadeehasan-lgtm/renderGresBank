package com.izak.demoBankManagement.repository;

import com.izak.demoBankManagement.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentSchedule, Long> {

    List<LoanRepaymentSchedule> findByLoanId(Long loanId);

    @Query("SELECT s FROM LoanRepaymentSchedule s WHERE s.loan.id = :loanId " +
            "AND s.status = :status ORDER BY s.installmentNumber")
    List<LoanRepaymentSchedule> findByLoanIdAndStatus(@Param("loanId") Long loanId,
                                                      @Param("status") LoanRepaymentSchedule.ScheduleStatus status);

    /**
     * Find all repayment schedules by loan ID ordered by due date ascending
     */
    @Query("SELECT s FROM LoanRepaymentSchedule s WHERE s.loan.id = :loanId ORDER BY s.dueDate ASC")
    List<LoanRepaymentSchedule> findByLoanIdOrderByDueDateAsc(@Param("loanId") Long loanId);

    @Query("SELECT s FROM LoanRepaymentSchedule s WHERE s.dueDate < :cutoffDate " +
            "AND s.status = 'PENDING'")
    List<LoanRepaymentSchedule> findOverdueInstallments(@Param("cutoffDate") LocalDate cutoffDate);

    @Query("SELECT s FROM LoanRepaymentSchedule s WHERE s.loan.loanId = :loanId " +
            "ORDER BY s.installmentNumber")
    List<LoanRepaymentSchedule> findByLoanIdOrderByInstallmentNumber(@Param("loanId") String loanId);
}

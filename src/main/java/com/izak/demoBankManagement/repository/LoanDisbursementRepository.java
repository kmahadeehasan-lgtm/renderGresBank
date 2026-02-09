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
public interface LoanDisbursementRepository extends JpaRepository<LoanDisbursement, Long> {

    List<LoanDisbursement> findByLoanId(Long loanId);

    @Query("SELECT d FROM LoanDisbursement d WHERE d.loan.id = :loanId AND d.status = :status")
    List<LoanDisbursement> findByLoanIdAndStatus(@Param("loanId") Long loanId,
                                                 @Param("status") LoanDisbursement.DisbursementStatus status);
}

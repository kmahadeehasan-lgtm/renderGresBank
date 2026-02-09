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
public interface LoanApprovalHistoryRepository extends JpaRepository<LoanApprovalHistory, Long> {

    @Query("SELECT h FROM LoanApprovalHistory h WHERE h.loan.id = :loanId " +
            "ORDER BY h.actionDate DESC")
    List<LoanApprovalHistory> findByLoanIdOrderByActionDateDesc(@Param("loanId") Long loanId);

    List<LoanApprovalHistory> findByLoanId(Long loanId);
}

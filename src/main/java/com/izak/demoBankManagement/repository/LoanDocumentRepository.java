package com.izak.demoBankManagement.repository;


import com.izak.demoBankManagement.entity.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {

    List<LoanDocument> findByLoanId(Long loanId);

    @Query("SELECT d FROM LoanDocument d WHERE d.loan.id = :loanId AND d.status = :status")
    List<LoanDocument> findByLoanIdAndVerificationStatus(@Param("loanId") Long loanId,
                                                         @Param("status") LoanDocument.DocumentStatus status);
}

package com.izak.demoBankManagement.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_disbursements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private LocalDate disbursementDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 50)
    private String transactionId; // FK to Transaction

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisbursementStatus status;

    @Column(length = 500)
    private String bankDetails;

    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String remarks;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    public enum DisbursementStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}
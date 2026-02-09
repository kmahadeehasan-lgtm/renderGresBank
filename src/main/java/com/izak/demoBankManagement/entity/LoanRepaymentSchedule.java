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
@Table(name = "loan_repayment_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate paymentDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.PENDING;

    @Column(length = 50)
    private String transactionId; // FK to Transaction

    @Column(precision = 10, scale = 2)
    private BigDecimal penaltyApplied = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal balanceAfterPayment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    public enum ScheduleStatus {
        PENDING,
        PAID,
        OVERDUE,
        WAIVED
    }
}
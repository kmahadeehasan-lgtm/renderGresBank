package com.izak.demoBankManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "dps_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DPS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dps_number", unique = true, nullable = false)
    private String dpsNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "customer_id_ref", nullable = false)
    private String customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_account_id")
    private Account linkedAccount; // Account for auto-debit

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "monthly_installment", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyInstallment;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths; // e.g., 12, 24, 36, 60 months

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate; // Annual interest rate

    @Column(name = "maturity_amount", precision = 15, scale = 2)
    private BigDecimal maturityAmount;

    @Column(name = "total_deposited", precision = 15, scale = 2)
    private BigDecimal totalDeposited = BigDecimal.ZERO;

    @Column(name = "total_installments_paid")
    private Integer totalInstallmentsPaid = 0;

    @Column(name = "pending_installments")
    private Integer pendingInstallments;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DPSStatus status = DPSStatus.ACTIVE;

    @Column(name = "auto_debit_enabled")
    private Boolean autoDebitEnabled = false;

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "missed_installments")
    private Integer missedInstallments = 0;

    @Column(name = "currency", nullable = false)
    private String currency = "USD";

    // Nominee Information
    @Column(name = "nominee_first_name")
    private String nomineeFirstName;

    @Column(name = "nominee_last_name")
    private String nomineeLastName;

    @Column(name = "nominee_relationship")
    private String nomineeRelationship;

    @Column(name = "nominee_phone")
    private String nomineePhone;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "closed_date")
    private LocalDateTime closedDate;

    @Column(name = "matured_date")
    private LocalDateTime maturedDate;

    // Relationships
    @OneToMany(mappedBy = "dps", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DPSInstallment> installments;

    public enum DPSStatus {
        ACTIVE,      // Currently active
        MATURED,     // Completed successfully
        CLOSED,      // Prematurely closed
        DEFAULTED,   // Too many missed payments
        SUSPENDED    // Temporarily suspended
    }
}
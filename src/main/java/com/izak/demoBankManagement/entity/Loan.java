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
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String loanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account; // Disbursement/repayment account

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicantType applicantType = ApplicantType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus loanStatus = LoanStatus.APPLICATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    // Financial fields
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principal;

    @Column(precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal disbursedAmount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal annualInterestRate;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Column(precision = 15, scale = 2)
    private BigDecimal monthlyEMI;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalInterest;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount;

    // Approval workflow
    private LocalDate approvedDate;

    @Column(length = 1000)
    private String approvalConditions;

    // Disbursement
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DisbursementStatus disbursementStatus = DisbursementStatus.PENDING;

    private LocalDate scheduledDisbursementDate;

    private LocalDate actualDisbursementDate;

    @Column(name = "disbursement_account_id")
    private Long disbursementAccountId;

    // Collateral (for secured loans)
    @Column(length = 100)
    private String collateralType;

    @Column(precision = 15, scale = 2)
    private BigDecimal collateralValue;

    @Column(length = 1000)
    private String collateralDescription;

    // Documents & KYC
    @Column(length = 2000)
    private String requiredDocumentsStatus; // JSON format

    @Column(nullable = false)
    private Boolean kycVerified = false;

    // Risk assessment
    private Integer creditScore;

    @Column(length = 50)
    private String eligibilityStatus;

    @Column(length = 20)
    private String riskRating;

    // Special fields for Import LC
    @Column(length = 100)
    private String lcNumber;

    @Column(length = 200)
    private String beneficiaryName;

    @Column(length = 200)
    private String beneficiaryBank;

    private LocalDate lcExpiryDate;

    @Column(precision = 15, scale = 2)
    private BigDecimal lcAmount;

    @Column(length = 500)
    private String purposeOfLC;

    @Column(length = 200)
    private String paymentTerms;

    // Special fields for Industrial/Working Capital
    @Column(length = 100)
    private String industryType;

    @Column(length = 100)
    private String businessRegistrationNumber;

    @Column(precision = 18, scale = 2)
    private BigDecimal businessTurnover;

    // General fields
    @Column(length = 500)
    private String purpose;

    @Column(length = 1000)
    private String remarks;

    @Column(length = 1000)
    private String rejectionReason;

    // Dates
    @Column(nullable = false)
    private LocalDate applicationDate;

    private LocalDate closedDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime lastModified;

    // Relationships
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanRepaymentSchedule> repaymentSchedules;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanApprovalHistory> approvalHistory;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanDocument> documents;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanDisbursement> disbursements;

    // Enums
    public enum LoanType {
        HOME_LOAN,
        CAR_LOAN,
        PERSONAL_LOAN,
        EDUCATION_LOAN,
        BUSINESS_LOAN,
        GOLD_LOAN,
        INDUSTRIAL_LOAN,
        IMPORT_LC_LOAN,
        WORKING_CAPITAL_LOAN
    }

    public enum ApplicantType {
        INDIVIDUAL,
        CORPORATE
    }

    public enum LoanStatus {
        APPLICATION,
        PROCESSING,
        APPROVED,
        ACTIVE,
        CLOSED,
        DEFAULTED
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum DisbursementStatus {
        PENDING,
        SCHEDULED,
        COMPLETED,
        FAILED
    }
}




//package com.izak.demoBankManagement.entity;
//
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "loans")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class Loan {
//
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(unique = true, nullable = false, length = 50)
//    private String loanId;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "customer_id", nullable = false)
//    private Customer customer;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "account_id")
//    private Account account;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 50)
//    private LoanType loanType;
//
//    @Column(nullable = false, precision = 15, scale = 2)
//    private BigDecimal amount;
//
//    @Column(nullable = false, precision = 5, scale = 2)
//    private BigDecimal interestRate;
//
//    @Column(nullable = false)
//    private Integer tenure; // in months
//
//    @Column(precision = 15, scale = 2)
//    private BigDecimal monthlyEmi;
//
//    @Column(precision = 15, scale = 2)
//    private BigDecimal totalInterest;
//
//    @Column(precision = 15, scale = 2)
//    private BigDecimal totalAmount;
//
//    @Column(precision = 15, scale = 2)
//    private BigDecimal outstandingBalance;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private Status status = Status.PENDING;
//
//    @Column(nullable = false)
//    private LocalDate applicationDate;
//
//    private LocalDate approvalDate;
//
//    private LocalDate disbursementDate;
//
//    private LocalDate maturityDate;
//
//    @Column(length = 50)
//    private String approvedBy;
//
//    @Column(length = 500)
//    private String purpose;
//
//    @Column(length = 500)
//    private String remarks;
//
//    @Column(length = 500)
//    private String rejectionReason;
//
//    @CreationTimestamp
//    @Column(nullable = false, updatable = false)
//    private LocalDateTime createdDate;
//
//    @UpdateTimestamp
//    private LocalDateTime lastModified;
//
//    // Enums
//    public enum LoanType {
//        HOME_LOAN,
//        CAR_LOAN,
//        PERSONAL_LOAN,
//        EDUCATION_LOAN,
//        BUSINESS_LOAN,
//        GOLD_LOAN
//    }
//
//    public enum Status {
//        PENDING,
//        APPROVED,
//        REJECTED,
//        ACTIVE,
//        CLOSED,
//        DEFAULTED
//    }
//}

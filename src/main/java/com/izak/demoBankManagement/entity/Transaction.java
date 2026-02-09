package com.izak.demoBankManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String transactionId;

    @Column(unique = true, length = 50)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = true)
    private Account fromAccount;

    @Column(nullable = true, length = 20)
    private String fromAccountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @Column(length = 20)
    private String toAccountNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransferType transferType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(precision = 10, scale = 2)
    private BigDecimal transferFee = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal serviceTax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferMode transferMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String remarks;

    @Column(length = 100)
    private String beneficiaryName;

    @Column(length = 100)
    private String beneficiaryBank;

    // REMOVED: String beneficiaryBranch field
    // Branch info will be accessed through fromAccount.getBranch() and toAccount.getBranch()

    @Column(precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime scheduledAt;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 255)
    private String deviceInfo;

    @Column(length = 100)
    private String location;

    @Column(length = 20)
    private String errorCode;

    @Column(length = 500)
    private String errorMessage;

    @Column(length = 50)
    private String receiptNumber;

    @Column(length = 500)
    private String receiptUrl;

    @Column(length = 50)
    private String createdBy;

    @Column(length = 50)
    private String modifiedBy;

    @UpdateTimestamp
    private LocalDateTime lastModified;

    private Boolean isScheduled = false;

    private Boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RecurringFrequency recurringFrequency;

    private LocalDateTime nextRecurringDate;

    private Integer riskScore;

    private Boolean fraudCheckPassed = true;

    private Boolean requiresApproval = false;

    @Column(length = 50)
    private String approvedBy;

    private LocalDateTime approvedAt;

    private Boolean verified = false;

    // Enums
    public enum TransactionType {
        TRANSFER,
        DEPOSIT,
        WITHDRAWAL,
        PAYMENT,
        REFUND
    }

    public enum TransferType {
        OWN,
        OTHER
    }

    public enum TransferMode {
        NEFT,
        RTGS,
        IMPS,
        UPI,
        CASH,
        CHEQUE,
        CARD
    }

    public enum Status {
        COMPLETED,
        PENDING,
        FAILED,
        CANCELLED,
        PROCESSING
    }

    public enum RecurringFrequency {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }
}

//public class Transaction {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(unique = true, nullable = false, length = 50)
//    private String transactionId;
//
//    @Column(unique = true, length = 50)
//    private String referenceNumber;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "from_account_id", nullable = false)
//    private Account fromAccount;
//
//    @Column(nullable = false, length = 20)
//    private String fromAccountNumber;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "to_account_id")
//    private Account toAccount;
//
//    @Column(length = 20)
//    private String toAccountNumber;
//
//    @Enumerated(EnumType.STRING)
//    @Column(length = 20)
//    private TransactionType transactionType;
//
//    @Enumerated(EnumType.STRING)
//    @Column(length = 20)
//    private TransferType transferType;
//
//    @Column(nullable = false, precision = 15, scale = 2)
//    private BigDecimal amount;
//
//    @Column(nullable = false, length = 3)
//    private String currency = "USD";
//
//    @Column(precision = 10, scale = 2)
//    private BigDecimal transferFee = BigDecimal.ZERO;
//
//    @Column(precision = 10, scale = 2)
//    private BigDecimal serviceTax = BigDecimal.ZERO;
//
//    @Column(nullable = false, precision = 15, scale = 2)
//    private BigDecimal totalAmount;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private TransferMode transferMode;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private Status status = Status.PENDING;
//
//    @Column(length = 500)
//    private String description;
//
//    @Column(length = 500)
//    private String remarks;
//
//    @Column(length = 100)
//    private String beneficiaryName;
//
//    @Column(length = 100)
//    private String beneficiaryBank;
//
//    @Column(length = 100)
//    private Branch beneficiaryBranch;
//
//    @Column(precision = 15, scale = 2)
//    private BigDecimal balanceBefore;
//
//    @Column(precision = 15, scale = 2)
//    private BigDecimal balanceAfter;
//
//    @CreationTimestamp
//    @Column(nullable = false, updatable = false)
//    private LocalDateTime timestamp;
//
//    private LocalDateTime completedAt;
//
//    private LocalDateTime cancelledAt;
//
//    private LocalDateTime scheduledAt;
//
//    @Column(length = 50)
//    private String ipAddress;
//
//    @Column(length = 255)
//    private String deviceInfo;
//
//    @Column(length = 100)
//    private String location;
//
//    @Column(length = 20)
//    private String errorCode;
//
//    @Column(length = 500)
//    private String errorMessage;
//
//    @Column(length = 50)
//    private String receiptNumber;
//
//    @Column(length = 500)
//    private String receiptUrl;
//
//    @Column(length = 50)
//    private String createdBy;
//
//    @Column(length = 50)
//    private String modifiedBy;
//
//    @UpdateTimestamp
//    private LocalDateTime lastModified;
//
//    private Boolean isScheduled = false;
//
//    private Boolean isRecurring = false;
//
//    @Enumerated(EnumType.STRING)
//    @Column(length = 20)
//    private RecurringFrequency recurringFrequency;
//
//    private LocalDateTime nextRecurringDate;
//
//    private Integer riskScore;
//
//    private Boolean fraudCheckPassed = true;
//
//    private Boolean requiresApproval = false;
//
//    @Column(length = 50)
//    private String approvedBy;
//
//    private LocalDateTime approvedAt;
//
//    private Boolean verified = false;
//
//    // Enums
//    public enum TransactionType {
//        TRANSFER,
//        DEPOSIT,
//        WITHDRAWAL,
//        PAYMENT,
//        REFUND
//    }
//
//    public enum TransferType {
//        OWN,
//        OTHER
//    }
//
//    public enum TransferMode {
//        NEFT,
//        RTGS,
//        IMPS,
//        UPI,
//        CASH,
//        CHEQUE,
//        CARD
//    }
//
//    public enum Status {
//        COMPLETED,
//        PENDING,
//        FAILED,
//        CANCELLED,
//        PROCESSING
//    }
//
//    public enum RecurringFrequency {
//        DAILY,
//        WEEKLY,
//        MONTHLY,
//        YEARLY
//    }
//}

package com.izak.demoBankManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "customer_identifier", nullable = false, length = 20)
    private String customerId;

    @Column(nullable = false, length = 50)
    private String accountType;

    // FIXED: Changed from String branch to Branch entity relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(length = 50)
    private String nomineeFirstName;

    @Column(length = 50)
    private String nomineeLastName;

    @Column(length = 50)
    private String nomineeRelationship;

    @Column(length = 20)
    private String nomineePhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycStatus kycStatus = KycStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    // Relationships
    @OneToMany(mappedBy = "fromAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> outgoingTransactions;

    @OneToMany(mappedBy = "toAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> incomingTransactions;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Loan> loans;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Card> cards;

    // Enums
    public enum Status {
        ACTIVE,
        INACTIVE,
        CLOSED,
        FROZEN
    }

    public enum KycStatus {
        PENDING,
        VERIFIED,
        REJECTED
    }
}
//public class Account {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(unique = true, nullable = false, length = 20)
//    private String accountNumber;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "customer_id", nullable = false)
//    private Customer customer;
//
//    @Column(name = "customer_identifier", nullable = false, length = 20)
//    private String customerId;
//
//    @Column(nullable = false, length = 50)
//    private String accountType;
//
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "branch_id")
//    private Branch branch;
//
//    @Column(nullable = false, precision = 15, scale = 2)
//    private BigDecimal balance = BigDecimal.ZERO;
//
//    @Column(nullable = false, length = 3)
//    private String currency = "USD";
//
//    @Column(nullable = false, precision = 5, scale = 2)
//    private BigDecimal interestRate = BigDecimal.ZERO;
//
//    @Column(length = 50)
//    private String nomineeFirstName;
//
//    @Column(length = 50)
//    private String nomineeLastName;
//
//    @Column(length = 50)
//    private String nomineeRelationship;
//
//    @Column(length = 20)
//    private String nomineePhone;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private Status status = Status.ACTIVE;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private KycStatus kycStatus = KycStatus.PENDING;
//
//    @CreationTimestamp
//    @Column(nullable = false, updatable = false)
//    private LocalDateTime createdDate;
//
//    @UpdateTimestamp
//    private LocalDateTime lastUpdated;
//
//    // Relationships
//    @OneToMany(mappedBy = "fromAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<Transaction> outgoingTransactions;
//
//    @OneToMany(mappedBy = "toAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<Transaction> incomingTransactions;
//
//    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<Loan> loans;
//
//    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<Card> cards;
//
//
//
//    // Enums
//    public enum Status {
//        ACTIVE,
//        INACTIVE,
//        CLOSED,
//        FROZEN
//    }
//
//    public enum KycStatus {
//        PENDING,
//        VERIFIED,
//        REJECTED
//    }
//}

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

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 16)
    private String cardNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardType cardType;

    @Column(nullable = false, length = 100)
    private String cardHolderName;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false, length = 4)
    private String cvv;

    @Column(nullable = false, length = 64)
    private String pin;

    @Column(precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(precision = 15, scale = 2)
    private BigDecimal availableLimit;

    @Column(precision = 15, scale = 2)
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(nullable = false)
    private LocalDate issueDate;

    private LocalDate activationDate;

    private LocalDate blockDate;

    @Column(length = 500)
    private String blockReason;

    @Column(nullable = false)
    private Boolean isInternational = false;

    @Column(nullable = false)
    private Boolean isOnlinePurchaseEnabled = true;

    @Column(nullable = false)
    private Boolean isContactless = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime lastModified;

    // Enums
    public enum CardType {
        DEBIT_CARD,
        CREDIT_CARD,
        PREPAID_CARD
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        BLOCKED,
        EXPIRED,
        CANCELLED
    }
}

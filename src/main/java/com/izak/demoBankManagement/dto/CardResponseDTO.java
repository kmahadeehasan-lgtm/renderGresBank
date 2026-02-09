package com.izak.demoBankManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class CardResponseDTO {

    private Long id;
    private String maskedCardNumber;
    private String cardHolderName;
    private String cardType;
    private LocalDate expiryDate;
    private String status;

    // Account and Customer info
    private String customerId;
    private String customerName;
    private Long accountId;
    private String accountNumber;

    // Card limits and features
    private BigDecimal creditLimit;
    private BigDecimal availableLimit;
    private BigDecimal outstandingBalance;
    private Boolean isInternational;
    private Boolean isOnlinePurchaseEnabled;
    private Boolean isContactless;

    // Dates
    private LocalDate issueDate;
    private LocalDate activationDate;
    private LocalDate blockDate;
    private String blockReason;

    // Audit
    private LocalDateTime createdDate;
    private LocalDateTime lastModified;

    // Temporary PIN - Only populated during card issuance
    private String temporaryPin;
}


//public class CardResponseDTO {
//
//    private Long id;
//    private String maskedCardNumber; // **** **** **** 1234
//    private String cardHolderName;
//    private String cardType;
//    private LocalDate expiryDate;
//    private String status;
//
//    // Account and Customer info
//    private String customerId;
//    private String customerName;
//    private Long accountId;
//    private String accountNumber;
//
//    // Card limits and features
//    private BigDecimal creditLimit;
//    private BigDecimal availableLimit;
//    private BigDecimal outstandingBalance;
//    private Boolean isInternational;
//    private Boolean isOnlinePurchaseEnabled;
//    private Boolean isContactless;
//
//    // Dates
//    private LocalDate issueDate;
//    private LocalDate activationDate;
//    private LocalDate blockDate;
//    private String blockReason;
//
//    // Audit
//    private LocalDateTime createdDate;
//    private LocalDateTime lastModified;
//}
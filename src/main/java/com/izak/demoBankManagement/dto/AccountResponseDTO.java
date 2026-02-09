package com.izak.demoBankManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor


public class AccountResponseDTO {

    private Long id;
    private String accountNumber;
    private String customerId;
    private String accountType;

    // Branch information - UPDATED
    private Long branchId;
    private String branchCode;
    private String branchName;
    private String branchCity;

    private BigDecimal balance;
    private String currency;
    private BigDecimal interestRate;
    private String nomineeFirstName;
    private String nomineeLastName;
    private String nomineeRelationship;
    private String nomineePhone;
    private String status;
    private String kycStatus;
    private LocalDateTime createdDate;
    private LocalDateTime lastUpdated;

    // Customer info
    private String customerName;
    private String customerEmail;

    // Transaction summary
    private Integer totalTransactions;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
}




//public class AccountResponseDTO {
//
//    private Long id;
//    private String accountNumber;
//    private String customerId;
//    private String accountType;
//    private String branch;
//    private BigDecimal balance;
//    private String currency;
//    private BigDecimal interestRate;
//    private String nomineeFirstName;
//    private String nomineeLastName;
//    private String nomineeRelationship;
//    private String nomineePhone;
//    private String status;
//    private String kycStatus;
//    private LocalDateTime createdDate;
//    private LocalDateTime lastUpdated;
//
//    // Customer info
//    private String customerName;
//    private String customerEmail;
//
//    // Transaction summary
//    private Integer totalTransactions;
//    private BigDecimal totalDeposits;
//    private BigDecimal totalWithdrawals;
//}
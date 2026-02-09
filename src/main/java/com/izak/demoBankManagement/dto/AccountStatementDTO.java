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
public class AccountStatementDTO {
    private String accountNumber;
    private String accountType;
    private String customerName;
    private String customerEmail;

    // Branch information - ADDED
    private String branchCode;
    private String branchName;
    private String branchAddress;

    private LocalDateTime statementStartDate;
    private LocalDateTime statementEndDate;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private Integer transactionCount;

    private java.util.List<TransactionHistoryDTO> transactions;
}










//public class AccountStatementDTO {
//    private String accountNumber;
//    private String accountType;
//    private String customerName;
//    private String customerEmail;
//    private String branch;
//    private LocalDateTime statementStartDate;
//    private LocalDateTime statementEndDate;
//    private BigDecimal openingBalance;
//    private BigDecimal closingBalance;
//    private BigDecimal totalCredits;
//    private BigDecimal totalDebits;
//    private Integer transactionCount;
//
//    // List of transactions
//    private java.util.List<TransactionHistoryDTO> transactions;
//}

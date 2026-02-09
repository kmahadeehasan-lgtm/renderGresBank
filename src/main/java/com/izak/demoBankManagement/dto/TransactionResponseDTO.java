package com.izak.demoBankManagement.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {

    private String transactionId;
    private String referenceNumber;
    private String fromAccountNumber;
    private String toAccountNumber;

    // Branch information - ADDED
    private String fromBranchCode;
    private String fromBranchName;
    private String toBranchCode;
    private String toBranchName;

    private BigDecimal amount;
    private String currency;
    private BigDecimal transferFee;
    private BigDecimal serviceTax;
    private BigDecimal totalAmount;
    private String transferMode;
    private String transactionType;
    private String status;
    private String description;
    private String remarks;
    private String timestamp;
    private String completedAt;
    private String receiptNumber;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String beneficiaryName;
    private String beneficiaryBank;

    // Additional info
    private boolean fraudCheckPassed;
    private boolean requiresApproval;
}



//public class TransactionResponseDTO {
//
//    private String transactionId;
//    private String referenceNumber;
//    private String fromAccountNumber;
//    private String toAccountNumber;
//    private BigDecimal amount;
//    private String currency;
//    private BigDecimal transferFee;
//    private BigDecimal serviceTax;
//    private BigDecimal totalAmount;
//    private String transferMode;
//    private String transactionType;
//    private String status;
//    private String description;
//    private String remarks;
//    private String timestamp;
//    private String completedAt;
//    private String receiptNumber;
//    private BigDecimal balanceBefore;
//    private BigDecimal balanceAfter;
//    private String beneficiaryName;
//    private String beneficiaryBank;
//
//    // Additional info
//    private boolean fraudCheckPassed;
//    private boolean requiresApproval;
//}

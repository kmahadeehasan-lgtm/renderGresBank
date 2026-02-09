package com.izak.demoBankManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryDTO {

    private String transactionId;
    private String referenceNumber;
    private String accountNumber;
    private String otherAccountNumber;

    // Branch information - ADDED
    private String branchCode;
    private String branchName;
    private String otherBranchCode;
    private String otherBranchName;

    private String transactionType; // DEBIT or CREDIT
    private BigDecimal amount;
    private String transferMode;
    private String status;
    private String description;
    private String timestamp;
    private BigDecimal balanceAfter;
}





//public class TransactionHistoryDTO {
//
//    private String transactionId;
//    private String referenceNumber;
//    private String accountNumber; // The account being queried
//    private String otherAccountNumber; // The other party's account
//    private String transactionType; // DEBIT or CREDIT (from perspective of queried account)
//    private BigDecimal amount;
//    private String transferMode;
//    private String status;
//    private String description;
//    private String timestamp;
//    private BigDecimal balanceAfter;
//}
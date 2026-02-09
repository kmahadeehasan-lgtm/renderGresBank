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
public class AccountListItemDTO {

    private Long id;
    private String accountNumber;
    private String customerId;
    private String customerName;
    private String accountType;

    // Branch information - UPDATED
    private Long branchId;
    private String branchCode;
    private String branchName;

    private BigDecimal balance;
    private String currency;
    private String status;
    private String kycStatus;
    private LocalDateTime createdDate;
}


//public class AccountListItemDTO {
//
//    private Long id;
//    private String accountNumber;
//    private String customerId;
//    private String customerName;
//    private String accountType;
//    private String branch;
//    private BigDecimal balance;
//    private String currency;
//    private String status;
//    private String kycStatus;
//    private LocalDateTime createdDate;
//}
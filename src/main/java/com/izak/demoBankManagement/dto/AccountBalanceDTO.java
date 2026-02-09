package com.izak.demoBankManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceDTO {

    private String accountNumber;
    private String customerId;
    private String accountType;

    // Branch information - ADDED
    private String branchCode;
    private String branchName;

    private BigDecimal balance;
    private String currency;
    private String status;
}

//public class AccountBalanceDTO {
//
//    private String accountNumber;
//    private String customerId;
//    private String accountType;
//    private BigDecimal balance;
//    private String currency;
//    private String status;
//}
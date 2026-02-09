package com.izak.demoBankManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ==================== LoanResponseDTO ====================
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponseDTO {

    private String loanId;
    private String loanType;
    private String loanStatus;
    private String approvalStatus;
    private BigDecimal principal;
    private BigDecimal annualInterestRate;
    private Integer tenureMonths;
    private BigDecimal monthlyEMI;
    private BigDecimal totalAmount;
    private BigDecimal totalInterest;
    private BigDecimal outstandingBalance;
    private BigDecimal disbursedAmount;
    private BigDecimal approvedAmount;
    private Integer creditScore;
    private String eligibilityStatus;
    private LocalDate applicationDate;
    private LocalDate approvedDate;
    private LocalDate actualDisbursementDate;
    private String customerId;
    private String customerName;
    private String accountNumber;
    private String collateralType;
    private BigDecimal collateralValue;
    private String purpose;
    private String approvalConditions;
    private LocalDateTime createdDate;
    private String disbursementStatus;

    // Special fields
    private String lcNumber;
    private String beneficiaryName;
    private String industryType;
    private BigDecimal businessTurnover;



}
package com.izak.demoBankManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanListItemDTO {

    private String loanId;
    private String loanType;
    private String loanStatus;
    private String approvalStatus;
    private BigDecimal principal;
    private BigDecimal outstandingBalance;
    private BigDecimal monthlyEMI;
    private LocalDate applicationDate;
    private String customerName;
    private String customerId;
}
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
public class LoanStatementResponseDTO {

    private String loanId;
    private String customerName;
    private String customerEmail;
    private String loanType;
    private BigDecimal principal;
    private BigDecimal annualInterestRate;
    private Integer tenureMonths;
    private BigDecimal monthlyEMI;
    private LocalDate applicationDate;
    private LocalDate disbursementDate;
    private BigDecimal totalAmount;
    private BigDecimal totalPaid;
    private BigDecimal outstandingBalance;
    private Integer installmentsPaid;
    private Integer installmentsPending;
    private LocalDate nextEMIDate;
    private BigDecimal nextEMIAmount;
    private List<RepaymentScheduleResponseDTO> repaymentSchedule;
    private List<DisbursementHistoryDTO> disbursementHistory;

    // FIX: Add this field
    private LoanResponseDTO loan;
}
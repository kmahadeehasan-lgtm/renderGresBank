package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
// DPS RESPONSE
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DPSResponseDTO {
    private Long id;
    private String dpsNumber;
    private String customerId;
    private String customerName;
    private String linkedAccountNumber;
    private String branchName;
    private BigDecimal monthlyInstallment;
    private Integer tenureMonths;
    private BigDecimal interestRate;
    private BigDecimal maturityAmount;
    private BigDecimal totalDeposited;
    private Integer totalInstallmentsPaid;
    private Integer pendingInstallments;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private LocalDate nextPaymentDate;
    private String status;
    private Boolean autoDebitEnabled;
    private BigDecimal penaltyAmount;
    private Integer missedInstallments;
    private String currency;
    private String nomineeFirstName;
    private String nomineeLastName;
    private LocalDateTime createdDate;
}

package com.izak.demoBankManagement.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

// DPS Maturity Calculation DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DPSMaturityCalculationDTO {
    private BigDecimal monthlyInstallment;
    private Integer tenureMonths;
    private BigDecimal interestRate;
    private BigDecimal totalDeposit;
    private BigDecimal interestEarned;
    private BigDecimal maturityAmount;
}
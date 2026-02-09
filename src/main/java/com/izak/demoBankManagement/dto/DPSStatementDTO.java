package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
// DPS STATEMENT
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DPSStatementDTO {
    private String dpsNumber;
    private String customerName;
    private BigDecimal monthlyInstallment;
    private Integer totalInstallments;
    private Integer paidInstallments;
    private Integer pendingInstallments;
    private BigDecimal totalDeposited;
    private BigDecimal maturityAmount;
    private LocalDate maturityDate;
    private List<DPSInstallmentDTO> installments;
}

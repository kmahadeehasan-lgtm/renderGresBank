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
public class RepaymentScheduleResponseDTO {

    private Integer installmentNumber;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalAmount;
    private String status;
    private BigDecimal balanceAfterPayment;
    private BigDecimal penaltyApplied;
}
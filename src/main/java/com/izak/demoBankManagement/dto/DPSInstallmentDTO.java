package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.*;
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
public class DPSInstallmentDTO {
    private Integer installmentNumber;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private BigDecimal penaltyAmount;
    private String status;
    private String transactionId;
    private String receiptNumber;
}
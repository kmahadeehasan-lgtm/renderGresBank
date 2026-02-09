package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
// PAY DPS INSTALLMENT
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DPSPaymentRequestDTO {

    @NotBlank(message = "DPS number is required")
    private String dpsNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode; // CASH, CARD, AUTO_DEBIT

    private String remarks;
}

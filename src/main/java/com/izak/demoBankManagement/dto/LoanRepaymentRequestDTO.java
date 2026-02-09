package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepaymentRequestDTO {

    @NotBlank(message = "Loan ID is required")
    private String loanId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "1.00", message = "Payment amount must be positive")
    private BigDecimal paymentAmount;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode; // CASH, CHEQUE, NEFT, IMPS

    private String transactionReference;
}
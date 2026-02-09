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
public class LoanDisbursementRequestDTO {

    @NotBlank(message = "Loan ID is required")
    private String loanId;

    @NotNull(message = "Disbursement amount is required")
    @DecimalMin(value = "1.00", message = "Disbursement amount must be positive")
    private BigDecimal disbursementAmount;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    private String bankDetails;

    private LocalDate scheduledDate;
}

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
public class LoanForeclosureRequestDTO {

    @NotBlank(message = "Loan ID is required")
    private String loanId;

    @NotNull(message = "Foreclosure date is required")
    private LocalDate foreclosureDate;

    @NotBlank(message = "Settlement account number is required")
    private String settlementAccountNumber;
}
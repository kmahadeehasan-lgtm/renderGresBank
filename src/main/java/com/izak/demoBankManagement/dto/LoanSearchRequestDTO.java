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
public class LoanSearchRequestDTO {

    private String customerId;

    private String loanStatus; // APPLICATION, PROCESSING, APPROVED, ACTIVE, CLOSED, DEFAULTED

    private String loanType; // HOME_LOAN, CAR_LOAN, etc.

    @Min(value = 0, message = "Page number cannot be negative")
    private Integer pageNumber = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer pageSize = 20;
}

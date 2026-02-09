package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// CREATE DPS
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DPSCreateRequestDTO {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    private String linkedAccountNumber; // For auto-debit

    @NotBlank(message = "Branch code is required")
    private String branchCode;

    @NotNull(message = "Monthly installment is required")
    @DecimalMin(value = "100.00", message = "Minimum installment is $100")
    private BigDecimal monthlyInstallment;

    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Minimum tenure is 6 months")
    @Max(value = 120, message = "Maximum tenure is 120 months")
    private Integer tenureMonths;

    @NotNull(message = "Interest rate is required")
    private BigDecimal interestRate;

    private Boolean autoDebitEnabled;

    // Nominee info
    private String nomineeFirstName;
    private String nomineeLastName;
    private String nomineeRelationship;
    private String nomineePhone;

    private String remarks;
}
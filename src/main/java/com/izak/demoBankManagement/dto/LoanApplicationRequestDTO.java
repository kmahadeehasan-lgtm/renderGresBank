package com.izak.demoBankManagement.dto;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// ==================== LoanApplicationRequestDTO ====================
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequestDTO {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Loan type is required")
    private String loanType; // HOME_LOAN, CAR_LOAN, etc.

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "50000.00", message = "Minimum loan amount is 50,000")
    @DecimalMax(value = "10000000.00", message = "Maximum loan amount is 10,000,000")
    private BigDecimal loanAmount;

    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Minimum tenure is 6 months")
    @Max(value = 360, message = "Maximum tenure is 360 months")
    private Integer tenureMonths;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "5.00", message = "Minimum interest rate is 5%")
    @DecimalMax(value = "25.00", message = "Maximum interest rate is 25%")
    private BigDecimal annualInterestRate;

    @NotBlank(message = "Account number for disbursement is required")
    private String accountNumber;

    private String applicantType; // INDIVIDUAL, CORPORATE

    // Collateral info (for secured loans)
    private String collateralType;

    @DecimalMin(value = "0.00", message = "Collateral value cannot be negative")
    private BigDecimal collateralValue;

    private String collateralDescription;

    // Applicant details
    @NotBlank(message = "Applicant name is required")
    private String applicantName;

    @NotNull(message = "Age is required")
    @Min(value = 21, message = "Minimum age is 21")
    @Max(value = 65, message = "Maximum age is 65")
    private Integer age;

    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "1000.00", message = "Minimum monthly income is 1,000")
    private BigDecimal monthlyIncome;

    @NotBlank(message = "Employment type is required")
    private String employmentType; // SALARIED, SELF_EMPLOYED, BUSINESS

    @Size(max = 500, message = "Purpose cannot exceed 500 characters")
    private String purpose;

    // Special fields for Import LC
    private String lcNumber;
    private String beneficiaryName;
    private String beneficiaryBank;
    private LocalDate lcExpiryDate;
    private BigDecimal lcAmount;
    private String purposeOfLC;
    private String paymentTerms;

    // Special fields for Industrial/Working Capital
    private String industryType;
    private String businessRegistrationNumber;
    private BigDecimal businessTurnover;

    // Document list
    private List<String> documentTypes;
}
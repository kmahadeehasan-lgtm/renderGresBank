package com.izak.demoBankManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================
// ACCOUNT CREATE REQUEST DTO
// ============================================
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreateRequestDTO {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Account type is required")
    private String accountType;

    @NotBlank(message = "Branch code is required")
    @Size(max = 20, message = "Branch code cannot exceed 20 characters")
    private String branchCode; // CHANGED from branch name to branch code

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    @Digits(integer = 15, fraction = 2, message = "Invalid balance format")
    private BigDecimal balance;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (e.g., USD)")
    private String currency;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
    @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100%")
    private BigDecimal interestRate;

    // Nominee Information (Optional)
    private String nomineeFirstName;
    private String nomineeLastName;
    private String nomineeRelationship;

    @Pattern(regexp = "^\\+880?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String nomineePhone;
}






//public class AccountCreateRequestDTO {
//
//    @NotBlank(message = "Customer ID is required")
//    private String customerId;
//
//    @NotBlank(message = "Account type is required")
//    private String accountType; // Savings, Current, Fixed Deposit, Recurring Deposit
//
//    @NotBlank(message = "Branch is required")
//    @Size(max = 100, message = "Branch name cannot exceed 100 characters")
////    private String branch;
//    private String branchCode;
//
//    @NotNull(message = "Initial balance is required")
//    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
//    @Digits(integer = 15, fraction = 2, message = "Invalid balance format")
//    private BigDecimal balance;
//
//    @NotBlank(message = "Currency is required")
//    @Size(min = 3, max = 3, message = "Currency must be 3 characters (e.g., USD)")
//    private String currency;
//
//    @NotNull(message = "Interest rate is required")
//    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
//    @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100%")
//    private BigDecimal interestRate;
//
//    // Nominee Information (Optional)
//    private String nomineeFirstName;
//    private String nomineeLastName;
//    private String nomineeRelationship;
//
//    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
//    private String nomineePhone;
//}
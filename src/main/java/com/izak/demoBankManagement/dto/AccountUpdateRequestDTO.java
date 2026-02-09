package com.izak.demoBankManagement.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateRequestDTO {

    @Size(max = 20, message = "Branch code cannot exceed 20 characters")
    private String branchCode; // CHANGED to allow branch transfer

    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
    @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100%")
    private BigDecimal interestRate;

    private String nomineeFirstName;
    private String nomineeLastName;
    private String nomineeRelationship;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String nomineePhone;

    private String status;
    private String kycStatus;
}


//public class AccountUpdateRequestDTO {
//
//    @Size(max = 100, message = "Branch name cannot exceed 100 characters")
//    private String branch;
//
//    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
//    @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100%")
//    private BigDecimal interestRate;
//
//    private String nomineeFirstName;
//    private String nomineeLastName;
//    private String nomineeRelationship;
//
//    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
//    private String nomineePhone;
//
//    private String status; // active, inactive, closed, frozen
//    private String kycStatus; // pending, verified, rejected
//}
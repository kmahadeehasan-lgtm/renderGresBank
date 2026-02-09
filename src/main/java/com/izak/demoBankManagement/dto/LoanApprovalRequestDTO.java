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
public class LoanApprovalRequestDTO {

    @NotBlank(message = "Loan ID is required")
    private String loanId;

    @NotBlank(message = "Approval status is required")
    private String approvalStatus; // APPROVED, REJECTED

    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    private String comments;

    @Size(max = 1000, message = "Approval conditions cannot exceed 1000 characters")
    private String approvalConditions;

    private BigDecimal interestRateModification;

    private String rejectionReason;
}
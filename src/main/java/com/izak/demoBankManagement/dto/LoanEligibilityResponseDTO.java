package com.izak.demoBankManagement.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanEligibilityResponseDTO {

    private Boolean isEligible;
    private Integer eligibilityScore; // 0-100
    private List<String> reasons;
    private BigDecimal recommendedLoanAmount;
    private BigDecimal recommendedInterestRate;
    private LocalDate nextReviewDate;
    private String riskRating;
}
package com.izak.demoBankManagement.service;




import com.izak.demoBankManagement.dto.LoanApplicationRequestDTO;
import com.izak.demoBankManagement.dto.LoanEligibilityResponseDTO;
import com.izak.demoBankManagement.entity.Account;
import com.izak.demoBankManagement.entity.Customer;
import com.izak.demoBankManagement.entity.Loan;
import com.izak.demoBankManagement.exception.LoanEligibilityException;
import com.izak.demoBankManagement.repository.AccountRepository;
import com.izak.demoBankManagement.repository.CustomerRepository;
import com.izak.demoBankManagement.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for validating loan eligibility based on multiple criteria
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanEligibilityService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final LoanCalculationService calculationService;

    // Eligibility criteria constants
    private static final int MIN_AGE = 21;
    private static final int MAX_AGE = 65;
    private static final BigDecimal MIN_MONTHLY_INCOME = new BigDecimal("1000.00");
    private static final BigDecimal MAX_DTI_RATIO = new BigDecimal("50.00"); // 50%
    private static final BigDecimal MIN_CREDIT_SCORE = new BigDecimal("600");
    private static final BigDecimal MAX_LTV_RATIO = new BigDecimal("80.00"); // 80%
    private static final BigDecimal MIN_COLLATERAL_VALUE_RATIO = new BigDecimal("120.00"); // 120% of loan

    /**
     * Check comprehensive loan eligibility
     */
    public LoanEligibilityResponseDTO checkEligibility(LoanApplicationRequestDTO request) {
        log.info("Checking loan eligibility for customer: {}", request.getCustomerId());

        List<String> reasons = new ArrayList<>();
        int eligibilityScore = 100;

        // Validate customer exists and is active
        Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new LoanEligibilityException(
                        "Customer not found", List.of("Customer ID is invalid")));

        if (customer.getStatus() != Customer.Status.ACTIVE) {
            reasons.add("Customer account is not active");
            eligibilityScore -= 100;
        }

        // Check KYC status
        if (customer.getKycStatus() != Customer.KycStatus.VERIFIED) {
            reasons.add("KYC verification is pending or rejected");
            eligibilityScore -= 30;
        }

        // Validate age
        int age = request.getAge();
        if (age < MIN_AGE || age > MAX_AGE) {
            reasons.add(String.format("Age must be between %d and %d years", MIN_AGE, MAX_AGE));
            eligibilityScore -= 25;
        }

        // Validate monthly income
        if (request.getMonthlyIncome().compareTo(MIN_MONTHLY_INCOME) < 0) {
            reasons.add(String.format("Minimum monthly income required: %s", MIN_MONTHLY_INCOME));
            eligibilityScore -= 20;
        }

        // Calculate and check DTI ratio
        BigDecimal existingEMI = calculateExistingEMI(request.getCustomerId());
        BigDecimal proposedEMI = calculationService.calculateEMI(
                request.getLoanAmount(),
                request.getAnnualInterestRate(),
                request.getTenureMonths()
        );
        BigDecimal totalEMI = existingEMI.add(proposedEMI);
        BigDecimal dtiRatio = calculationService.calculateDTI(totalEMI, request.getMonthlyIncome());

        log.debug("DTI Calculation - Existing EMI: {}, Proposed EMI: {}, Total EMI: {}, Income: {}, DTI: {}%",
                existingEMI, proposedEMI, totalEMI, request.getMonthlyIncome(), dtiRatio);

        if (dtiRatio.compareTo(MAX_DTI_RATIO) > 0) {
            reasons.add(String.format("Debt-to-Income ratio (%.2f%%) exceeds maximum allowed (%.2f%%)",
                    dtiRatio, MAX_DTI_RATIO));
            eligibilityScore -= 30;
        }

        // Check collateral for secured loans
        boolean isSecuredLoan = isSecuredLoanType(request.getLoanType());
        if (isSecuredLoan) {
            if (request.getCollateralValue() == null ||
                    request.getCollateralValue().compareTo(BigDecimal.ZERO) <= 0) {
                reasons.add("Collateral is required for this loan type");
                eligibilityScore -= 40;
            } else {
                BigDecimal ltvRatio = calculationService.calculateLTV(
                        request.getLoanAmount(),
                        request.getCollateralValue()
                );

                if (ltvRatio.compareTo(MAX_LTV_RATIO) > 0) {
                    reasons.add(String.format("Loan-to-Value ratio (%.2f%%) exceeds maximum (%.2f%%)",
                            ltvRatio, MAX_LTV_RATIO));
                    eligibilityScore -= 25;
                }

                BigDecimal collateralRatio = request.getCollateralValue()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(request.getLoanAmount(), 2, RoundingMode.HALF_UP);

                if (collateralRatio.compareTo(MIN_COLLATERAL_VALUE_RATIO) < 0) {
                    reasons.add(String.format("Collateral value must be at least %s%% of loan amount",
                            MIN_COLLATERAL_VALUE_RATIO));
                    eligibilityScore -= 20;
                }
            }
        }

        // Check existing loan defaults
        List<Loan> existingLoans = loanRepository.findByCustomerIdAndStatus(
                request.getCustomerId(), Loan.LoanStatus.DEFAULTED);
        if (!existingLoans.isEmpty()) {
            reasons.add("Customer has defaulted loans");
            eligibilityScore -= 50;
        }

        // Validate linked account
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new LoanEligibilityException(
                        "Account not found", List.of("Invalid account number")));

        if (!account.getCustomerId().equals(request.getCustomerId())) {
            reasons.add("Account does not belong to the customer");
            eligibilityScore -= 100;
        }

        if (account.getStatus() != Account.Status.ACTIVE) {
            reasons.add("Linked account is not active");
            eligibilityScore -= 30;
        }

        // Ensure score doesn't go below 0
        eligibilityScore = Math.max(0, eligibilityScore);

        // Determine eligibility
        boolean isEligible = eligibilityScore >= 70 && reasons.isEmpty();

        // Calculate recommended values
        BigDecimal recommendedAmount = calculateRecommendedAmount(
                request.getMonthlyIncome(), existingEMI, request.getTenureMonths(),
                request.getAnnualInterestRate());
        BigDecimal recommendedRate = calculateRecommendedRate(eligibilityScore);

        // Build response
        LoanEligibilityResponseDTO response = new LoanEligibilityResponseDTO();
        response.setIsEligible(isEligible);
        response.setEligibilityScore(eligibilityScore);
        response.setReasons(reasons);
        response.setRecommendedLoanAmount(recommendedAmount);
        response.setRecommendedInterestRate(recommendedRate);
        response.setNextReviewDate(LocalDate.now().plusMonths(3));
        response.setRiskRating(calculateRiskRating(eligibilityScore));

        log.info("Eligibility check completed - Eligible: {}, Score: {}, Reasons: {}",
                isEligible, eligibilityScore, reasons.size());

        return response;
    }

    /**
     * Calculate total existing EMI for the customer
     */
    private BigDecimal calculateExistingEMI(String customerId) {
        List<Loan> activeLoans = loanRepository.findByCustomerIdAndStatus(
                customerId, Loan.LoanStatus.ACTIVE);

        return activeLoans.stream()
                .map(Loan::getMonthlyEMI)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if loan type requires collateral
     */
    private boolean isSecuredLoanType(String loanType) {
        return loanType.equalsIgnoreCase("HOME_LOAN") ||
                loanType.equalsIgnoreCase("CAR_LOAN") ||
                loanType.equalsIgnoreCase("GOLD_LOAN") ||
                loanType.equalsIgnoreCase("INDUSTRIAL_LOAN");
    }

    /**
     * Calculate recommended loan amount based on income and DTI
     */
    private BigDecimal calculateRecommendedAmount(BigDecimal monthlyIncome,
                                                  BigDecimal existingEMI,
                                                  Integer tenureMonths,
                                                  BigDecimal annualRate) {
        // Calculate maximum EMI allowed (40% of income for conservative approach)
        BigDecimal maxAllowedEMI = monthlyIncome
                .multiply(new BigDecimal("0.40"))
                .subtract(existingEMI);

        if (maxAllowedEMI.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Calculate loan amount for this EMI
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = pow(onePlusRate, tenureMonths);
        BigDecimal denominator = monthlyRate.multiply(power);
        BigDecimal numerator = power.subtract(BigDecimal.ONE);

        BigDecimal recommendedAmount = maxAllowedEMI
                .multiply(numerator)
                .divide(denominator, 2, RoundingMode.HALF_UP);

        return recommendedAmount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate recommended interest rate based on eligibility score
     */
    private BigDecimal calculateRecommendedRate(int eligibilityScore) {
        if (eligibilityScore >= 90) {
            return new BigDecimal("7.50");
        } else if (eligibilityScore >= 80) {
            return new BigDecimal("8.50");
        } else if (eligibilityScore >= 70) {
            return new BigDecimal("9.50");
        } else {
            return new BigDecimal("11.50");
        }
    }

    /**
     * Calculate risk rating based on eligibility score
     */
    private String calculateRiskRating(int eligibilityScore) {
        if (eligibilityScore >= 90) {
            return "LOW";
        } else if (eligibilityScore >= 75) {
            return "MODERATE";
        } else if (eligibilityScore >= 60) {
            return "HIGH";
        } else {
            return "VERY_HIGH";
        }
    }

    /**
     * Power function for BigDecimal
     */
    private BigDecimal pow(BigDecimal base, int exponent) {
        if (exponent == 0) {
            return BigDecimal.ONE;
        }
        BigDecimal result = base;
        for (int i = 1; i < exponent; i++) {
            result = result.multiply(base);
        }
        return result.setScale(10, RoundingMode.HALF_UP);
    }
}

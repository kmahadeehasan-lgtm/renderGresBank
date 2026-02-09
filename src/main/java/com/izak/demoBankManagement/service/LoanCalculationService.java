package com.izak.demoBankManagement.service;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for loan financial calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanCalculationService {

    /**
     * Calculate EMI using reduce-balance method
     * Formula: EMI = P × r × (1+r)^n / ((1+r)^n - 1)
     * where P = Principal, r = Monthly rate, n = Number of months
     */
    public BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualRate, Integer months) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal must be positive");
        }
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Interest rate must be positive");
        }
        if (months == null || months <= 0) {
            throw new IllegalArgumentException("Tenure must be positive");
        }

        // Convert annual rate to monthly rate
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        // Calculate (1 + r)
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);

        // Calculate (1 + r)^n using power calculation
        BigDecimal power = pow(onePlusRate, months);

        // Calculate numerator: P × r × (1+r)^n
        BigDecimal numerator = principal
                .multiply(monthlyRate)
                .multiply(power);

        // Calculate denominator: (1+r)^n - 1
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        // Calculate EMI
        BigDecimal emi = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

        log.debug("Calculated EMI: {} for Principal: {}, Rate: {}%, Tenure: {} months",
                emi, principal, annualRate, months);

        return emi;
    }

    /**
     * Calculate total interest
     */
    public BigDecimal calculateTotalInterest(BigDecimal emi, Integer months, BigDecimal principal) {
        BigDecimal totalAmount = emi.multiply(BigDecimal.valueOf(months));
        return totalAmount.subtract(principal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total amount (principal + interest)
     */
    public BigDecimal calculateTotalAmount(BigDecimal emi, Integer months) {
        return emi.multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate interest for a specific period
     */
    public BigDecimal calculatePeriodInterest(BigDecimal outstandingBalance, BigDecimal annualRate) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        return outstandingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate principal for a specific period
     */
    public BigDecimal calculatePeriodPrincipal(BigDecimal emi, BigDecimal periodInterest) {
        return emi.subtract(periodInterest).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate prepayment charges
     */
    public BigDecimal calculatePrepaymentCharges(BigDecimal outstandingBalance,
                                                 BigDecimal chargePercentage) {
        return outstandingBalance
                .multiply(chargePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate late payment penalty
     */
    public BigDecimal calculateLatePenalty(BigDecimal overdueAmount,
                                           BigDecimal penaltyRate,
                                           Integer daysOverdue) {
        // Penalty = Overdue Amount × Penalty Rate × (Days / 30)
        BigDecimal monthlyPenalty = overdueAmount
                .multiply(penaltyRate)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        BigDecimal dailyPenalty = monthlyPenalty
                .divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP);

        return dailyPenalty
                .multiply(BigDecimal.valueOf(daysOverdue))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate outstanding balance after payment
     */
    public BigDecimal calculateOutstandingAfterPayment(BigDecimal currentOutstanding,
                                                       BigDecimal principalPaid) {
        return currentOutstanding.subtract(principalPaid).setScale(2, RoundingMode.HALF_UP);
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

    /**
     * Calculate Loan-to-Value (LTV) ratio
     */
    public BigDecimal calculateLTV(BigDecimal loanAmount, BigDecimal collateralValue) {
        if (collateralValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return loanAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(collateralValue, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Debt-to-Income (DTI) ratio
     */
    public BigDecimal calculateDTI(BigDecimal totalEMI, BigDecimal monthlyIncome) {
        if (monthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }

        return totalEMI
                .multiply(BigDecimal.valueOf(100))
                .divide(monthlyIncome, 2, RoundingMode.HALF_UP);
    }
}

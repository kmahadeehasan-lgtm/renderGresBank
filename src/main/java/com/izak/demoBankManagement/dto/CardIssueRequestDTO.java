package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardIssueRequestDTO {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotBlank(message = "Card type is required")
    @Pattern(regexp = "DEBIT_CARD|CREDIT_CARD|PREPAID_CARD", message = "Invalid card type")
    private String cardType;

    @DecimalMin(value = "0.0", message = "Credit limit cannot be negative")
    @DecimalMax(value = "1000000.0", message = "Credit limit cannot exceed 1,000,000")
    private BigDecimal creditLimit; // Required for CREDIT_CARD

    @NotNull(message = "International card flag is required")
    private Boolean isInternational;

    @NotNull(message = "Online purchase flag is required")
    private Boolean isOnlinePurchaseEnabled;

    @NotNull(message = "Contactless flag is required")
    private Boolean isContactless;
}
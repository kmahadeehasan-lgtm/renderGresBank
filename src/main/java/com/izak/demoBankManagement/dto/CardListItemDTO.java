package com.izak.demoBankManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardListItemDTO {

    private Long id;
    private String maskedCardNumber;
    private String cardHolderName;
    private String cardType;
    private String status;
    private LocalDate expiryDate;
    private BigDecimal creditLimit;
    private BigDecimal availableLimit;
    private String customerId;
    private String accountNumber;
    private Boolean isInternational;
}

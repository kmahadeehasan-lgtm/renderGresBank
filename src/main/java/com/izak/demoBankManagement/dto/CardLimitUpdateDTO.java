package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardLimitUpdateDTO {

    @NotNull(message = "Credit limit is required")
    @DecimalMin(value = "0.0", message = "Credit limit cannot be negative")
    @DecimalMax(value = "1000000.0", message = "Credit limit cannot exceed 1,000,000")
    private BigDecimal creditLimit;
}

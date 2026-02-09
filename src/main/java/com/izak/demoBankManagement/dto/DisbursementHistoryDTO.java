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
public class DisbursementHistoryDTO {

    private LocalDate disbursementDate;
    private BigDecimal amount;
    private String transactionId;
    private String status;
    private String reference;




}
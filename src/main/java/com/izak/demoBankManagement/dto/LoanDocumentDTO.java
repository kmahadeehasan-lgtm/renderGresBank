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
public class LoanDocumentDTO {

    private String documentType;
    private LocalDate submissionDate;
    private LocalDate verificationDate;
    private String status;
    private String remarks;
}

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
public class LoanApprovalHistoryDTO {

    private String approvalStage;
    private LocalDateTime actionDate;
    private String actionByUser;
    private String decision;
    private String comments;
    private String approvalConditions;
}
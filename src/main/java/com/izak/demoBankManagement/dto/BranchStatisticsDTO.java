package com.izak.demoBankManagement.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchStatisticsDTO {
    private Long branchId;
    private String branchCode;
    private String branchName;
    private String city;
    private Integer totalAccounts;
    private Integer activeAccounts;
    private BigDecimal totalBalance;
    private BigDecimal averageBalance;
}
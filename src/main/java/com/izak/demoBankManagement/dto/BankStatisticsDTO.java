package com.izak.demoBankManagement.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankStatisticsDTO {
    private Integer totalBranches;
    private Integer activeBranches;
    private Long totalAccounts;
    private Long activeAccounts;
    private BigDecimal totalBankBalance;
    private BigDecimal averageBalancePerAccount;
    private List<BranchStatisticsDTO> branchStatistics;
}

package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
// BRANCH RESPONSE
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponseDTO {
    private Long id;
    private String branchCode;
    private String branchName;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phone;
    private String email;
    private String managerName;
    private String managerPhone;
    private String managerEmail;
    private String ifscCode;
    private String swiftCode;
    private String status;
    private String workingHours;
    private Boolean isMainBranch;
    private LocalDateTime createdDate;
    private LocalDateTime lastUpdated;

    // Summary
    private Integer totalAccounts;
    private Integer totalDPS;
    private Integer totalEmployees;
}


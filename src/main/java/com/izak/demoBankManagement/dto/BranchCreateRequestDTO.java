package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// CREATE BRANCH
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchCreateRequestDTO {

    @NotBlank(message = "Branch name is required")
    @Size(max = 100)
    private String branchName;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zip code is required")
    private String zipCode;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    private String managerName;
    private String managerPhone;
    private String managerEmail;

    @NotBlank(message = "IFSC code is required")
    private String ifscCode;

    private String swiftCode;
    private String workingHours;
    private Boolean isMainBranch;
}

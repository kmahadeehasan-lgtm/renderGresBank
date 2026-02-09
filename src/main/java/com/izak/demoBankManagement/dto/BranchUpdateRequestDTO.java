package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
// UPDATE BRANCH
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchUpdateRequestDTO {
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
    private String workingHours;
    private String status;
}

package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardStatusUpdateDTO {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|BLOCKED|CANCELLED", message = "Invalid status. Must be ACTIVE, BLOCKED, or CANCELLED")
    private String status;

    private String reason; // Required for BLOCKED and CANCELLED
}
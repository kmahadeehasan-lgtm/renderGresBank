package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardPinUpdateDTO {

    @NotBlank(message = "Old PIN is required")
    @Size(min = 4, max = 4, message = "Old PIN must be exactly 4 digits")
    @Pattern(regexp = "\\d{4}", message = "Old PIN must contain only digits")
    private String oldPin;

    @NotBlank(message = "New PIN is required")
    @Size(min = 4, max = 4, message = "New PIN must be exactly 4 digits")
    @Pattern(regexp = "\\d{4}", message = "New PIN must contain only digits")
    private String newPin;
}

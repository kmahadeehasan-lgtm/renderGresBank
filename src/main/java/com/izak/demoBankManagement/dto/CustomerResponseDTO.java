package com.izak.demoBankManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDTO {

    private Long id;
    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String status;
    private String kycStatus;
    private String image;
    private LocalDateTime createdDate;
    private LocalDateTime lastUpdated;

    // User info
    private String username;
    private Boolean isActive;

    // Summary info
    private Integer totalAccounts;
    private Integer totalLoans;
    private Integer totalCards;
}

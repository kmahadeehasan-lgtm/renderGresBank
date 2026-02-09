package com.izak.demoBankManagement.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerListItemDTO {
    private Long id;
    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String city;
    private String state;
    private String status;
    private String kycStatus;
    private String image;
    private LocalDateTime createdDate;
}

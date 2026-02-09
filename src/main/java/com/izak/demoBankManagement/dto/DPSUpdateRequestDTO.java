
package com.izak.demoBankManagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
// UPDATE DPS
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DPSUpdateRequestDTO {
    private String linkedAccountNumber;
    private Boolean autoDebitEnabled;
    private String nomineeFirstName;
    private String nomineeLastName;
    private String nomineeRelationship;
    private String nomineePhone;
    private String status;
    private String remarks;
}

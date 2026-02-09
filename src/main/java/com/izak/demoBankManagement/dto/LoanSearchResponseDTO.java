package com.izak.demoBankManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanSearchResponseDTO {

    private List<LoanListItemDTO> loans;
    private Integer totalCount;
    private Integer pageNumber;
    private Integer pageSize;
    private Integer totalPages;
}
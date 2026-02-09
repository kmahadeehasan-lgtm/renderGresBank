package com.izak.demoBankManagement.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Entity
@Table(name = "loan_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentType documentType;

    @Column(nullable = false)
    private LocalDate submissionDate;

    private LocalDate verificationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(length = 500)
    private String remarks;

    @Column(length = 500)
    private String documentUrl;

    public enum DocumentType {
        INCOME_PROOF,
        ID_PROOF,
        ADDRESS_PROOF,
        PROPERTY_PAPERS,
        VEHICLE_DOCUMENTS,
        BUSINESS_REGISTRATION,
        TAX_RETURNS,
        BANK_STATEMENTS,
        COLLATERAL_DOCUMENTS,
        OTHER
    }

    public enum DocumentStatus {
        PENDING,
        VERIFIED,
        REJECTED
    }
}
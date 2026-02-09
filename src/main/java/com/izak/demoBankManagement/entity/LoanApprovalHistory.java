package com.izak.demoBankManagement.entity;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_approval_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApprovalStage approvalStage;

    @Column(nullable = false)
    private LocalDateTime actionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_by_user_id", nullable = false)
    private User actionBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Decision decision;

    @Column(length = 1000)
    private String comments;

    @Column(length = 1000)
    private String approvalConditions;

    public enum ApprovalStage {
        APPLICATION_REVIEW,
        DOCUMENT_VERIFICATION,
        CREDIT_CHECK,
        FINAL_APPROVAL
    }

    public enum Decision {
        APPROVED,
        REJECTED,
        PENDING
    }
}
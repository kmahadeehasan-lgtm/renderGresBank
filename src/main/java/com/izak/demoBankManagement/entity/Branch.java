package com.izak.demoBankManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "branches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String branchCode;

    @Column(nullable = false)
    private String branchName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String zipCode;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String email;

    private String managerName;
    private String managerPhone;
    private String managerEmail;

    @Column(name = "ifsc_code", unique = true)
    private String ifscCode;

    @Column(name = "swift_code")
    private String swiftCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BranchStatus status = BranchStatus.ACTIVE;

    @Column(name = "working_hours")
    private String workingHours; // e.g., "9:00 AM - 5:00 PM"

    @Column(name = "is_main_branch")
    private Boolean isMainBranch = false;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Relationships
    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL)
    private List<Account> accounts;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL)
    private List<DPS> dpsAccounts;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL)
    private List<Employee> employees;

    public enum BranchStatus {
        ACTIVE, INACTIVE, UNDER_MAINTENANCE
    }
}
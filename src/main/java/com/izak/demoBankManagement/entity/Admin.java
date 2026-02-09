package com.izak.demoBankManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "admins")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(unique = true, nullable = false, length = 20)
    private String adminId;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminLevel level = AdminLevel.STANDARD;

    @Column(length = 100)
    private String branch; // If admin is assigned to specific branch

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(length = 500)
    private String permissions; // JSON string or comma-separated permissions

    @Column(length = 500)
    private String image;

    private LocalDateTime lastLoginDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime lastModified;

    // Enums
    public enum AdminLevel {
        SUPER_ADMIN("super_admin"),      // Full system access
        ADMIN("admin"),                   // Standard admin access
        STANDARD("standard"),             // Basic admin operations
        BRANCH_ADMIN("branch_admin");     // Branch-specific admin

        private final String value;

        AdminLevel(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Status {
        ACTIVE("active"),
        INACTIVE("inactive"),
        SUSPENDED("suspended");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
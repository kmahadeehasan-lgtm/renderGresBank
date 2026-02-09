package com.izak.demoBankManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", unique = true, nullable = false)
    private String employeeId;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String address;

    private String city;
    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeRole role;

    @Column(nullable = false)
    private String department;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "salary", precision = 15, scale = 2)
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    private String image;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    public enum EmployeeRole {
        MANAGER,
        ASSISTANT_MANAGER,
        CASHIER,
        LOAN_OFFICER,
        CUSTOMER_SERVICE,
        ACCOUNTANT,
        SECURITY,
        IT_SUPPORT
    }

    public enum EmployeeStatus {
        ACTIVE, INACTIVE, ON_LEAVE, TERMINATED
    }
}



//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "employees")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class Employee {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @OneToOne
//    @JoinColumn(name = "user_id", nullable = false, unique = true)
//    private User user;
//
//    @Column(unique = true, nullable = false, length = 20)
//    private String employeeId;
//
//    @Column(nullable = false, length = 50)
//    private String firstName;
//
//    @Column(nullable = false, length = 50)
//    private String lastName;
//
//    @Column(nullable = false, length = 100)
//    private String email;
//
//    @Column(nullable = false, length = 20)
//    private String phone;
//
//    @Column(nullable = false, length = 100)
//    private String department;
//
//    @Column(nullable = false, length = 100)
//    private String position;
//
//    @Column(nullable = false, length = 100)
//    private String branch;
//
//    @Column(nullable = false)
//    private LocalDate hireDate;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private Status status = Status.ACTIVE;
//
//    @CreationTimestamp
//    @Column(nullable = false, updatable = false)
//    private LocalDateTime createdDate;
//
//    @UpdateTimestamp
//    private LocalDateTime lastModified;
//
//    // Enum
//    public enum Status {
//        ACTIVE,
//        INACTIVE,
//        ON_LEAVE,
//        TERMINATED
//    }
//}

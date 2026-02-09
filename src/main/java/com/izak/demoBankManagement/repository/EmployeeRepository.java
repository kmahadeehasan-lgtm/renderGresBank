package com.izak.demoBankManagement.repository;

import com.izak.demoBankManagement.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeId(String employeeId);
    Optional<Employee> findByEmail(String email);
    List<Employee> findByBranchId(Long branchId);
    List<Employee> findByRole(Employee.EmployeeRole role);
    List<Employee> findByStatus(Employee.EmployeeStatus status);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByEmail(String email);
}

package com.izak.demoBankManagement.controller;

import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.service.CustomerService;
import com.izak.demoBankManagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class CustomerController {

    private final CustomerService customerService;
    private final JwtUtil jwtUtil;

    // Public endpoint - handled by AuthController now
    // Kept here for backward compatibility
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> createCustomer(
            @Valid @RequestBody CustomerCreateRequestDTO request) {
        log.info("Create customer request received: {} {}", request.getFirstName(), request.getLastName());
        CustomerResponseDTO response = customerService.createCustomer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<CustomerListItemDTO>>> getAllCustomers() {
        log.info("Get all customers request");
        List<CustomerListItemDTO> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getCustomerById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        log.info("Get customer by ID request: {}", id);

        String jwt = token.substring(7);
        String role = jwtUtil.extractRole(jwt);
        String customerId = jwtUtil.extractCustomerId(jwt);

        CustomerResponseDTO customer = customerService.getCustomerById(id);

        // If customer, verify they're requesting their own data
        if ("CUSTOMER".equals(role) && !customer.getCustomerId().equals(customerId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You can only view your own profile"));
        }

        return ResponseEntity.ok(ApiResponse.success("Customer retrieved successfully", customer));
    }

    @GetMapping("/customer-id/{customerId}")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getCustomerByCustomerId(
            @PathVariable String customerId,
            @RequestHeader("Authorization") String token) {
        log.info("Get customer by customer ID request: {}", customerId);

        String jwt = token.substring(7);
        String role = jwtUtil.extractRole(jwt);
        String tokenCustomerId = jwtUtil.extractCustomerId(jwt);

        // If customer, verify they're requesting their own data
        if ("CUSTOMER".equals(role) && !customerId.equals(tokenCustomerId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You can only view your own profile"));
        }

        CustomerResponseDTO customer = customerService.getCustomerByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved successfully", customer));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<CustomerListItemDTO>>> getCustomersByStatus(
            @PathVariable String status) {
        log.info("Get customers by status request: {}", status);
        List<CustomerListItemDTO> customers = customerService.getCustomersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customers));
    }

    @GetMapping("/kyc-status/{kycStatus}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<CustomerListItemDTO>>> getCustomersByKycStatus(
            @PathVariable String kycStatus) {
        log.info("Get customers by KYC status request: {}", kycStatus);
        List<CustomerListItemDTO> customers = customerService.getCustomersByKycStatus(kycStatus);
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customers));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<CustomerListItemDTO>>> searchCustomers(
            @RequestParam("q") String searchTerm) {
        log.info("Search customers request: {}", searchTerm);
        List<CustomerListItemDTO> customers = customerService.searchCustomers(searchTerm);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", customers));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateRequestDTO request,
            @RequestHeader("Authorization") String token) {
        log.info("Update customer request for ID: {}", id);

        String jwt = token.substring(7);
        String role = jwtUtil.extractRole(jwt);
        String customerId = jwtUtil.extractCustomerId(jwt);

        CustomerResponseDTO existingCustomer = customerService.getCustomerById(id);

        // If customer, verify they're updating their own data
        if ("CUSTOMER".equals(role)) {
            if (!existingCustomer.getCustomerId().equals(customerId)) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied: You can only update your own profile"));
            }
            // Customers cannot change status or KYC status
            request.setStatus(null);
            request.setKycStatus(null);
        }

        CustomerResponseDTO response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", response));
    }

    @PatchMapping("/{customerId}/kyc-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> updateKycStatus(
            @PathVariable String customerId,
            @RequestParam String status) {
        log.info("Update KYC status request for customer: {} to {}", customerId, status);
        CustomerResponseDTO response = customerService.updateKycStatus(customerId, status);
        return ResponseEntity.ok(ApiResponse.success("KYC status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        log.info("Delete customer request for ID: {}", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> hardDeleteCustomer(@PathVariable Long id) {
        log.info("Permanent delete customer request for ID: {}", id);
        customerService.hardDeleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer permanently deleted", null));
    }
}

//package com.izak.demoBankManagement.controller;
//
//import com.izak.demoBankManagement.dto.*;
//import com.izak.demoBankManagement.service.CustomerService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//import jakarta.validation.Valid;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/customers")
//@RequiredArgsConstructor
//@Validated
//@Slf4j
//@CrossOrigin(origins = "http://localhost:4200")
//public class CustomerController {
//
//    private final CustomerService customerService;
//
//    // ============================================
//    // CREATE CUSTOMER
//    // POST /api/customers
//    // ============================================
//    @PostMapping
//    public ResponseEntity<ApiResponse<CustomerResponseDTO>> createCustomer(
//            @Valid @RequestBody CustomerCreateRequestDTO request) {
//
//        log.info("Create customer request received: {} {}", request.getFirstName(), request.getLastName());
//
//        CustomerResponseDTO response = customerService.createCustomer(request);
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Customer created successfully", response));
//    }
//
//    // ============================================
//    // GET ALL CUSTOMERS
//    // GET /api/customers
//    // ============================================
//    @GetMapping
//    public ResponseEntity<ApiResponse<List<CustomerListItemDTO>>> getAllCustomers() {
//        log.info("Get all customers request");
//
//        List<CustomerListItemDTO> customers = customerService.getAllCustomers();
//        return ResponseEntity
//                .ok(ApiResponse.success("Customers retrieved successfully", customers));
//    }
//
//    // ============================================
//    // GET CUSTOMER BY ID
//    // GET /api/customers/{id}
//    // ============================================
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getCustomerById(@PathVariable Long id) {
//        log.info("Get customer by ID request: {}", id);
//
//        CustomerResponseDTO customer = customerService.getCustomerById(id);
//        return ResponseEntity
//                .ok(ApiResponse.success("Customer retrieved successfully", customer));
//    }
//
//    // ============================================
//    // GET CUSTOMER BY CUSTOMER ID
//    // GET /api/customers/customer-id/{customerId}
//    // ============================================
//    @GetMapping("/customer-id/{customerId}")
//    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getCustomerByCustomerId(
//            @PathVariable String customerId) {
//        log.info("Get customer by customer ID request: {}", customerId);
//
//        CustomerResponseDTO customer = customerService.getCustomerByCustomerId(customerId);
//        return ResponseEntity
//                .ok(ApiResponse.success("Customer retrieved successfully", customer));
//    }
//
//    // ============================================
//    // GET CUSTOMERS BY STATUS
//    // GET /api/customers/status/{status}
//    // ============================================
//    @GetMapping("/status/{status}")
//    public ResponseEntity<ApiResponse<List<CustomerListItemDTO>>> getCustomersByStatus(
//            @PathVariable String status) {
//        log.info("Get customers by status request: {}", status);
//
//        List<CustomerListItemDTO> customers = customerService.getCustomersByStatus(status);
//        return ResponseEntity
//                .ok(ApiResponse.success("Customers retrieved successfully", customers));
//    }
//
//    // ============================================
//    // GET CUSTOMERS BY KYC STATUS
//    // GET /api/customers/kyc-status/{kycStatus}
//    // ============================================
//    @GetMapping("/kyc-status/{kycStatus}")
//    public ResponseEntity<ApiResponse<List<CustomerListItemDTO>>> getCustomersByKycStatus(
//            @PathVariable String kycStatus) {
//        log.info("Get customers by KYC status request: {}", kycStatus);
//
//        List<CustomerListItemDTO> customers = customerService.getCustomersByKycStatus(kycStatus);
//        return ResponseEntity
//                .ok(ApiResponse.success("Customers retrieved successfully", customers));
//    }
//
//    // ============================================
//    // SEARCH CUSTOMERS
//    // GET /api/customers/search?q={searchTerm}
//    // ============================================
//    @GetMapping("/search")
//    public ResponseEntity<ApiResponse<List<CustomerListItemDTO>>> searchCustomers(
//            @RequestParam("q") String searchTerm) {
//        log.info("Search customers request: {}", searchTerm);
//
//        List<CustomerListItemDTO> customers = customerService.searchCustomers(searchTerm);
//        return ResponseEntity
//                .ok(ApiResponse.success("Search results retrieved successfully", customers));
//    }
//
//    // ============================================
//    // UPDATE CUSTOMER
//    // PUT /api/customers/{id}
//    // ============================================
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<CustomerResponseDTO>> updateCustomer(
//            @PathVariable Long id,
//            @Valid @RequestBody CustomerUpdateRequestDTO request) {
//
//        log.info("Update customer request for ID: {}", id);
//
//        CustomerResponseDTO response = customerService.updateCustomer(id, request);
//        return ResponseEntity
//                .ok(ApiResponse.success("Customer updated successfully", response));
//    }
//
//    // ============================================
//    // UPDATE KYC STATUS
//    // PATCH /api/customers/{customerId}/kyc-status
//    // ============================================
//    @PatchMapping("/{customerId}/kyc-status")
//    public ResponseEntity<ApiResponse<CustomerResponseDTO>> updateKycStatus(
//            @PathVariable String customerId,
//            @RequestParam String status) {
//
//        log.info("Update KYC status request for customer: {} to {}", customerId, status);
//
//        CustomerResponseDTO response = customerService.updateKycStatus(customerId, status);
//        return ResponseEntity
//                .ok(ApiResponse.success("KYC status updated successfully", response));
//    }
//
//    // ============================================
//    // DELETE CUSTOMER (Soft Delete)
//    // DELETE /api/customers/{id}
//    // ============================================
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
//        log.info("Delete customer request for ID: {}", id);
//
//        customerService.deleteCustomer(id);
//        return ResponseEntity
//                .ok(ApiResponse.success("Customer deleted successfully", null));
//    }
//
//    // ============================================
//    // HARD DELETE CUSTOMER
//    // DELETE /api/customers/{id}/permanent
//    // ============================================
//    @DeleteMapping("/{id}/permanent")
//    public ResponseEntity<ApiResponse<Void>> hardDeleteCustomer(@PathVariable Long id) {
//        log.info("Permanent delete customer request for ID: {}", id);
//
//        customerService.hardDeleteCustomer(id);
//        return ResponseEntity
//                .ok(ApiResponse.success("Customer permanently deleted", null));
//    }
//}

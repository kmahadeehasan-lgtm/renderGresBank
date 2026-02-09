package com.izak.demoBankManagement.controller;

import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.service.LoanService;
import com.izak.demoBankManagement.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class LoanController {

    private final LoanService loanService;
    private final JwtUtil jwtUtil;

    // ============================================
    // CUSTOMER ENDPOINTS
    // ============================================

    /**
     * Apply for a new loan
     * POST /api/loans/apply
     */
    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> applyForLoan(
            @Valid @RequestBody LoanApplicationRequestDTO request,
            @RequestHeader("Authorization") String token) {

        log.info("Loan application request for customer: {}", request.getCustomerId());

        String jwt = token.substring(7);
        String role = jwtUtil.extractRole(jwt);
        String customerId = jwtUtil.extractCustomerId(jwt);

        // Verify ownership for customers
        if ("CUSTOMER".equals(role) && !request.getCustomerId().equals(customerId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You can only apply for loans for yourself"));
        }

        LoanResponseDTO response = loanService.applyForLoan(request, jwt);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Loan application submitted successfully", response));
    }

    /**
     * Get all loans for the authenticated customer
     * GET /api/loans/my-loans
     */
    @GetMapping("/my-loans")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<LoanListItemDTO>>> getMyLoans(
            @RequestHeader("Authorization") String token) {

        String jwt = token.substring(7);
        String customerId = jwtUtil.extractCustomerId(jwt);

        log.info("Getting loans for customer: {}", customerId);

        // FIX: Pass JWT token as second parameter
        List<LoanListItemDTO> loans = loanService.getLoansByCustomerId(customerId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully", loans));
    }
    /**
     * Get loan details by ID
     * GET /api/loans/{loanId}
     * MODIFIED: Now passes JWT token to service layer
     */
    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> getLoanById(
            @PathVariable String loanId,
            @RequestHeader("Authorization") String token) {

        log.info("Get loan details request: {}", loanId);

        // Extract JWT token without "Bearer " prefix
        String jwt = token.substring(7);
        String role = jwtUtil.extractRole(jwt);
        String customerId = jwtUtil.extractCustomerId(jwt);

        // Pass JWT token to service layer for branch authorization
        LoanResponseDTO loan = loanService.getLoanById(loanId, jwt);

        // Verify ownership for customers
        if ("CUSTOMER".equals(role) && !loan.getCustomerId().equals(customerId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You can only view your own loans"));
        }

        return ResponseEntity.ok(ApiResponse.success("Loan retrieved successfully", loan));
    }

    /**
     * Get loan statement with repayment schedule
     * GET /api/loans/{loanId}/statement
     */
    /**
     * Get loan statement with repayment schedule
     * GET /api/loans/{loanId}/statement
     * MODIFIED: Now passes JWT token to service layer for branch authorization
     */
    @GetMapping("/{loanId}/statement")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<LoanStatementResponseDTO>> getLoanStatement(
            @PathVariable String loanId,
            @RequestHeader("Authorization") String token) {

        log.info("Loan statement request: {}", loanId);

        // Extract JWT token without "Bearer " prefix
        String jwt = token.substring(7);
        String role = jwtUtil.extractRole(jwt);
        String customerId = jwtUtil.extractCustomerId(jwt);

        // Pass JWT token to service layer for branch authorization
        LoanStatementResponseDTO statement = loanService.getLoanStatement(loanId, jwt);

        // Verify ownership for customers
        if ("CUSTOMER".equals(role) && !statement.getLoan().getCustomerId().equals(customerId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You can only view your own loan statements"));
        }

        return ResponseEntity.ok(ApiResponse.success("Loan statement generated successfully", statement));
    }
    /**
     * Make loan repayment (EMI payment)
     * POST /api/loans/{loanId}/repay
     */
    /**
     * Make loan repayment (EMI payment)
     * POST /api/loans/{loanId}/repay
     * MODIFIED: Now passes JWT token to service layer for branch authorization
     */
    @PostMapping("/{loanId}/repay")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> repayLoan(
            @PathVariable String loanId,
            @Valid @RequestBody LoanRepaymentRequestDTO request,
            @RequestHeader("Authorization") String token) {

        log.info("Loan repayment request for loan: {}", loanId);

        // Extract JWT token without "Bearer " prefix
        String jwt = token.substring(7);
        String role = jwtUtil.extractRole(jwt);
        String customerId = jwtUtil.extractCustomerId(jwt);

        // Ensure loanId in path matches request body
        request.setLoanId(loanId);

        // Verify ownership for customers
        if ("CUSTOMER".equals(role)) {
            LoanResponseDTO loan = loanService.getLoanById(loanId, jwt);
            if (!loan.getCustomerId().equals(customerId)) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied: You can only repay your own loans"));
            }
        }

        // Pass JWT token to service layer for branch authorization
        TransactionResponseDTO response = loanService.repayLoan(request, jwt);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Loan repayment processed successfully", response));
    }

    /**
     * Foreclose loan (early closure)
     * POST /api/loans/{loanId}/foreclose
     */
    /**
     * Foreclose loan (early closure)
     * POST /api/loans/{loanId}/foreclose
     * MODIFIED: Now passes JWT token to service layer for branch authorization
     */
    @PostMapping("/{loanId}/foreclose")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> foreCloseLoan(
            @PathVariable String loanId,
            @Valid @RequestBody LoanForeclosureRequestDTO request,
            @RequestHeader("Authorization") String token) {

        log.info("Loan foreclosure request for loan: {}", loanId);

        String jwt = token.substring(7);
        String role = jwtUtil.extractRole(jwt);
        String customerId = jwtUtil.extractCustomerId(jwt);

        // Ensure loanId in path matches request body
        request.setLoanId(loanId);

        // Verify ownership for customers
        if ("CUSTOMER".equals(role)) {
            LoanResponseDTO loan = loanService.getLoanById(loanId, jwt);
            if (!loan.getCustomerId().equals(customerId)) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied: You can only foreclose your own loans"));
            }
        }

        // FIX: Changed from loanService.() to loanService.foreCloseLoan()
        LoanResponseDTO response = loanService.foreCloseLoan(request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Loan foreclosed successfully", response));
    }
    // ============================================
    // EMPLOYEE/ADMIN ENDPOINTS
    // ============================================

    /**
     * Get all loans by customer ID
     * GET /api/loans/customer/{customerId}
     */
    /**
     * Get all loans by customer ID
     * GET /api/loans/customer/{customerId}
     * MODIFIED: Now passes JWT token to service layer for branch authorization
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<List<LoanListItemDTO>>> getLoansByCustomerId(
            @PathVariable String customerId,
            @RequestHeader("Authorization") String token) {

        log.info("Get loans for customer: {}", customerId);

        // Extract JWT token without "Bearer " prefix
        String jwt = token.substring(7);

        // Pass JWT token to service layer for branch authorization
        List<LoanListItemDTO> loans = loanService.getLoansByCustomerId(customerId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully", loans));
    }

    /**
     * Get all pending approval loans
     * GET /api/loans/pending-approval
     */
    /**
     * Get all pending approval loans
     * GET /api/loans/pending-approval
     * MODIFIED: Now passes JWT token to service layer for branch authorization
     */
    @GetMapping("/pending-approval")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<List<LoanListItemDTO>>> getPendingApprovalLoans(
            @RequestHeader("Authorization") String token) {

        log.info("Get pending approval loans request");

        // Extract JWT token without "Bearer " prefix
        String jwt = token.substring(7);

        // Pass JWT token to service layer for branch authorization
        List<LoanListItemDTO> loans = loanService.getPendingApprovalLoans(jwt);
        return ResponseEntity.ok(ApiResponse.success(
                "Pending approval loans retrieved successfully", loans));
    }
    /**
     * Search loans with filters and pagination
     * POST /api/loans/search
     */
    /**
     * Search loans with filters and pagination
     * POST /api/loans/search
     * MODIFIED: Now passes JWT token to service layer for branch authorization
     */
    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<LoanSearchResponseDTO>> searchLoans(
            @Valid @RequestBody LoanSearchRequestDTO request,
            @RequestHeader("Authorization") String token) {

        log.info("Loan search request - Customer: {}, Status: {}, Type: {}",
                request.getCustomerId(), request.getLoanStatus(), request.getLoanType());

        // Extract JWT token without "Bearer " prefix
        String jwt = token.substring(7);

        // Pass JWT token to service layer for branch authorization
        LoanSearchResponseDTO response = loanService.searchLoans(request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", response));
    }
    /**
     * Get all loans (paginated)
     * GET /api/loans
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<LoanSearchResponseDTO>> getAllLoans(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String token) {

        log.info("Get all loans request - Page: {}, Size: {}", pageNumber, pageSize);

        // Extract JWT token without "Bearer " prefix
        String jwt = token.substring(7);

        // FIX: Pass JWT token to service layer
        LoanSearchResponseDTO response = loanService.getAllLoans(pageNumber, pageSize, jwt);
        return ResponseEntity.ok(ApiResponse.success("All loans retrieved successfully", response));
    }
    /**
     * Approve a loan
     * POST /api/loans/{loanId}/approve
     * MODIFIED: Now passes JWT token to service layer
     */
    @PostMapping("/{loanId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> approveLoan(
            @PathVariable String loanId,
            @Valid @RequestBody LoanApprovalRequestDTO request,
            @RequestHeader("Authorization") String token) {

        log.info("Loan approval request for loan: {}", loanId);

        // Extract JWT token without "Bearer " prefix
        String jwt = token.substring(7);

        // Ensure loanId in path matches request body
        request.setLoanId(loanId);
        request.setApprovalStatus("APPROVED");

        // Pass JWT token to service layer for branch authorization
        LoanResponseDTO response = loanService.approveLoan(request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Loan approved successfully", response));
    }

    /**
     * Reject a loan
     * POST /api/loans/{loanId}/reject
     * MODIFIED: Now passes JWT token to service layer
     */
    @PostMapping("/{loanId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> rejectLoan(
            @PathVariable String loanId,
            @Valid @RequestBody LoanApprovalRequestDTO request,
            @RequestHeader("Authorization") String token) {

        log.info("Loan rejection request for loan: {}", loanId);

        // Extract JWT token without "Bearer " prefix
        String jwt = token.substring(7);

        // Ensure loanId in path matches request body
        request.setLoanId(loanId);
        request.setApprovalStatus("REJECTED");

        // Pass JWT token to service layer for branch authorization
        LoanResponseDTO response = loanService.rejectLoan(request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Loan rejected successfully", response));
    }

    /**
     * Disburse a loan
     * POST /api/loans/{loanId}/disburse
     * MODIFIED: Now passes JWT token to service layer
     */
    @PostMapping("/{loanId}/disburse")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> disburseLoan(
            @PathVariable String loanId,
            @Valid @RequestBody LoanDisbursementRequestDTO request,
            @RequestHeader("Authorization") String token) {

        log.info("Loan disbursement request for loan: {}", loanId);

        // Extract JWT token without "Bearer " prefix
        String jwt = token.substring(7);

        // Ensure loanId in path matches request body
        request.setLoanId(loanId);

        // Pass JWT token to service layer for branch authorization
        LoanResponseDTO response = loanService.disburseLoan(request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Loan disbursed successfully", response));
    }

    /**
     * Mark defaulted loans (manual trigger for scheduled task)
     * POST /api/loans/mark-defaults
     */
    @PostMapping("/mark-defaults")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markDefaults(
            @RequestHeader("Authorization") String token) {
        log.info("Manual trigger for mark defaults process");

        // Extract JWT token without "Bearer " prefix
        String jwt = token.substring(7);

        // Pass JWT token to service layer for branch authorization
        loanService.markDefaults(jwt);
        return ResponseEntity.ok(ApiResponse.success("Default marking process completed", null));
    }

    // ============================================
    // HEALTH CHECK
    // ============================================

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Loan service is running", "OK"));
    }
}


















//package com.izak.demoBankManagement.controller;
//
//
//import com.izak.demoBankManagement.dto.*;
//import com.izak.demoBankManagement.service.LoanService;
//import com.izak.demoBankManagement.security.JwtUtil;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/loans")
//@RequiredArgsConstructor
//@Validated
//@Slf4j
//@CrossOrigin(origins = "http://localhost:4200")
//public class LoanController {
//
//    private final LoanService loanService;
//    private final JwtUtil jwtUtil;
//
//    // ============================================
//    // CUSTOMER ENDPOINTS
//    // ============================================
//
//    /**
//     * Apply for a new loan
//     * POST /api/loans/apply
//     */
//    @PostMapping("/apply")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<LoanResponseDTO>> applyForLoan(
//            @Valid @RequestBody LoanApplicationRequestDTO request,
//            @RequestHeader("Authorization") String token) {
//
//        log.info("Loan application request for customer: {}", request.getCustomerId());
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role) && !request.getCustomerId().equals(customerId)) {
//            return ResponseEntity
//                    .status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.error("Access denied: You can only apply for loans for yourself"));
//        }
//
//        LoanResponseDTO response = loanService.applyForLoan(request);
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Loan application submitted successfully", response));
//    }
//
//    /**
//     * Get all loans for the authenticated customer
//     * GET /api/loans/my-loans
//     */
//    @GetMapping("/my-loans")
//    @PreAuthorize("hasRole('CUSTOMER')")
//    public ResponseEntity<ApiResponse<List<LoanListItemDTO>>> getMyLoans(
//            @RequestHeader("Authorization") String token) {
//
//        String jwt = token.substring(7);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        log.info("Getting loans for customer: {}", customerId);
//
//        List<LoanListItemDTO> loans = loanService.getLoansByCustomerId(customerId);
//        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully", loans));
//    }
//
//    /**
//     * Get loan details by ID
//     * GET /api/loans/{loanId}
//     */
//    @GetMapping("/{loanId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<LoanResponseDTO>> getLoanById(
//            @PathVariable String loanId,
//            @RequestHeader("Authorization") String token) {
//
//        log.info("Get loan details request: {}", loanId);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        LoanResponseDTO loan = loanService.getLoanById(loanId);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role) && !loan.getCustomerId().equals(customerId)) {
//            return ResponseEntity
//                    .status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.error("Access denied: You can only view your own loans"));
//        }
//
//        return ResponseEntity.ok(ApiResponse.success("Loan retrieved successfully", loan));
//    }
//
//    /**
//     * Get loan statement with repayment schedule
//     * GET /api/loans/{loanId}/statement
//     */
//    @GetMapping("/{loanId}/statement")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<LoanStatementResponseDTO>> getLoanStatement(
//            @PathVariable String loanId,
//            @RequestHeader("Authorization") String token) {
//
//        log.info("Loan statement request: {}", loanId);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        LoanStatementResponseDTO statement = loanService.getLoanStatement(loanId);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role)) {
//            LoanResponseDTO loan = loanService.getLoanById(loanId);
//            if (!loan.getCustomerId().equals(customerId)) {
//                return ResponseEntity
//                        .status(HttpStatus.FORBIDDEN)
//                        .body(ApiResponse.error("Access denied: You can only view your own loan statements"));
//            }
//        }
//
//        return ResponseEntity.ok(ApiResponse.success("Loan statement generated successfully", statement));
//    }
//
//    /**
//     * Make loan repayment (EMI payment)
//     * POST /api/loans/{loanId}/repay
//     */
//    @PostMapping("/{loanId}/repay")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<TransactionResponseDTO>> repayLoan(
//            @PathVariable String loanId,
//            @Valid @RequestBody LoanRepaymentRequestDTO request,
//            @RequestHeader("Authorization") String token) {
//
//        log.info("Loan repayment request for loan: {}", loanId);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        // Ensure loanId in path matches request body
//        request.setLoanId(loanId);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role)) {
//            LoanResponseDTO loan = loanService.getLoanById(loanId);
//            if (!loan.getCustomerId().equals(customerId)) {
//                return ResponseEntity
//                        .status(HttpStatus.FORBIDDEN)
//                        .body(ApiResponse.error("Access denied: You can only repay your own loans"));
//            }
//        }
//
//        TransactionResponseDTO response = loanService.repayLoan(request);
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Loan repayment processed successfully", response));
//    }
//
//    /**
//     * Foreclose loan (early closure)
//     * POST /api/loans/{loanId}/foreclose
//     */
//    @PostMapping("/{loanId}/foreclose")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<LoanResponseDTO>> foreCloseLoan(
//            @PathVariable String loanId,
//            @Valid @RequestBody LoanForeclosureRequestDTO request,
//            @RequestHeader("Authorization") String token) {
//
//        log.info("Loan foreclosure request for loan: {}", loanId);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        // Ensure loanId in path matches request body
//        request.setLoanId(loanId);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role)) {
//            LoanResponseDTO loan = loanService.getLoanById(loanId);
//            if (!loan.getCustomerId().equals(customerId)) {
//                return ResponseEntity
//                        .status(HttpStatus.FORBIDDEN)
//                        .body(ApiResponse.error("Access denied: You can only foreclose your own loans"));
//            }
//        }
//
//        LoanResponseDTO response = loanService.foreCloseLoan(request);
//        return ResponseEntity.ok(ApiResponse.success("Loan foreclosed successfully", response));
//    }
//
//    // ============================================
//    // EMPLOYEE/ADMIN ENDPOINTS
//    // ============================================
//
//    /**
//     * Get all loans by customer ID
//     * GET /api/loans/customer/{customerId}
//     */
//    @GetMapping("/customer/{customerId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
//    public ResponseEntity<ApiResponse<List<LoanListItemDTO>>> getLoansByCustomerId(
//            @PathVariable String customerId) {
//
//        log.info("Get loans for customer: {}", customerId);
//
//        List<LoanListItemDTO> loans = loanService.getLoansByCustomerId(customerId);
//        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully", loans));
//    }
//
//    /**
//     * Get all pending approval loans
//     * GET /api/loans/pending-approval
//     */
//    @GetMapping("/pending-approval")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
//    public ResponseEntity<ApiResponse<List<LoanListItemDTO>>> getPendingApprovalLoans() {
//        log.info("Get pending approval loans request");
//
//        List<LoanListItemDTO> loans = loanService.getPendingApprovalLoans();
//        return ResponseEntity.ok(ApiResponse.success(
//                "Pending approval loans retrieved successfully", loans));
//    }
//
//    /**
//     * Search loans with filters and pagination
//     * POST /api/loans/search
//     */
//    @PostMapping("/search")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
//    public ResponseEntity<ApiResponse<LoanSearchResponseDTO>> searchLoans(
//            @Valid @RequestBody LoanSearchRequestDTO request) {
//
//        log.info("Loan search request - Customer: {}, Status: {}, Type: {}",
//                request.getCustomerId(), request.getLoanStatus(), request.getLoanType());
//
//        LoanSearchResponseDTO response = loanService.searchLoans(request);
//        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", response));
//    }
//
//    /**
//     * Get all loans (paginated)
//     * GET /api/loans
//     */
//    @GetMapping
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
//    public ResponseEntity<ApiResponse<LoanSearchResponseDTO>> getAllLoans(
//            @RequestParam(defaultValue = "1") int pageNumber,
//            @RequestParam(defaultValue = "10") int pageSize) {
//
//        log.info("Get all loans request - Page: {}, Size: {}", pageNumber, pageSize);
//
//        LoanSearchResponseDTO response = loanService.getAllLoans(pageNumber, pageSize);
//        return ResponseEntity.ok(ApiResponse.success("All loans retrieved successfully", response));
//    }
//
//
//
//    /**
//     * Approve a loan
//     * POST /api/loans/{loanId}/approve
//     */
//    @PostMapping("/{loanId}/approve")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
//    public ResponseEntity<ApiResponse<LoanResponseDTO>> approveLoan(
//            @PathVariable String loanId,
//            @Valid @RequestBody LoanApprovalRequestDTO request) {
//
//        log.info("Loan approval request for loan: {}", loanId);
//
//        // Ensure loanId in path matches request body
//        request.setLoanId(loanId);
//        request.setApprovalStatus("APPROVED");
//
//        LoanResponseDTO response = loanService.approveLoan(request);
//        return ResponseEntity.ok(ApiResponse.success("Loan approved successfully", response));
//    }
//
//    /**
//     * Reject a loan
//     * POST /api/loans/{loanId}/reject
//     */
//    @PostMapping("/{loanId}/reject")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
//    public ResponseEntity<ApiResponse<LoanResponseDTO>> rejectLoan(
//            @PathVariable String loanId,
//            @Valid @RequestBody LoanApprovalRequestDTO request) {
//
//        log.info("Loan rejection request for loan: {}", loanId);
//
//        // Ensure loanId in path matches request body
//        request.setLoanId(loanId);
//        request.setApprovalStatus("REJECTED");
//
//        LoanResponseDTO response = loanService.rejectLoan(request);
//        return ResponseEntity.ok(ApiResponse.success("Loan rejected successfully", response));
//    }
//
//    /**
//     * Disburse a loan
//     * POST /api/loans/{loanId}/disburse
//     */
//    @PostMapping("/{loanId}/disburse")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
//    public ResponseEntity<ApiResponse<LoanResponseDTO>> disburseLoan(
//            @PathVariable String loanId,
//            @Valid @RequestBody LoanDisbursementRequestDTO request) {
//
//        log.info("Loan disbursement request for loan: {}", loanId);
//
//        // Ensure loanId in path matches request body
//        request.setLoanId(loanId);
//
//        LoanResponseDTO response = loanService.disburseLoan(request);
//        return ResponseEntity.ok(ApiResponse.success("Loan disbursed successfully", response));
//    }
//
//    /**
//     * Mark defaulted loans (manual trigger for scheduled task)
//     * POST /api/loans/mark-defaults
//     */
//    @PostMapping("/mark-defaults")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ApiResponse<Void>> markDefaults() {
//        log.info("Manual trigger for mark defaults process");
//
//        loanService.markDefaults();
//        return ResponseEntity.ok(ApiResponse.success("Default marking process completed", null));
//    }
//
//    // ============================================
//    // HEALTH CHECK
//    // ============================================
//
//    @GetMapping("/health")
//    public ResponseEntity<ApiResponse<String>> healthCheck() {
//        return ResponseEntity.ok(ApiResponse.success("Loan service is running", "OK"));
//    }
//}
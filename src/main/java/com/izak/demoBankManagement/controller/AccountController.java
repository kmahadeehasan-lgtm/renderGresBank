package com.izak.demoBankManagement.controller;

import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.service.AccountService;
import com.izak.demoBankManagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class AccountController {

    private final AccountService accountService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> createAccount(
            @Valid @RequestBody AccountCreateRequestDTO request) {
        log.info("Create account request received for customer: {}", request.getCustomerId());
        AccountResponseDTO response = accountService.createAccount(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", response));
    }

//    @GetMapping
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
//    public ResponseEntity<ApiResponse<List<AccountListItemDTO>>> getAllAccounts() {
//        log.info("Get all accounts request");
//        List<AccountListItemDTO> accounts = accountService.getAllAccounts();
//        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
//    }

//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<AccountResponseDTO>> getAccountById(
//            @PathVariable Long id,
//            @RequestHeader("Authorization") String token) {
//        log.info("Get account by ID request: {}", id);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        AccountResponseDTO account = accountService.getAccountById(id);
//
//        // If customer, verify ownership
//        if ("CUSTOMER".equals(role) && !account.getCustomerId().equals(customerId)) {
//            return ResponseEntity
//                    .status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.error("Access denied: You can only view your own accounts"));
//        }
//
//        return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully", account));
//    }

//    @GetMapping("/account-number/{accountNumber}")
//    public ResponseEntity<ApiResponse<AccountResponseDTO>> getAccountByAccountNumber(
//            @PathVariable String accountNumber,
//            @RequestHeader("Authorization") String token) {
//        log.info("Get account by account number request: {}", accountNumber);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        AccountResponseDTO account = accountService.getAccountByAccountNumber(accountNumber);
//
//        // If customer, verify ownership
//        if ("CUSTOMER".equals(role) && !account.getCustomerId().equals(customerId)) {
//            return ResponseEntity
//                    .status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.error("Access denied: You can only view your own accounts"));
//        }
//
//        return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully", account));
//    }

//    @GetMapping("/customer/{customerId}")
//    public ResponseEntity<ApiResponse<List<AccountListItemDTO>>> getAccountsByCustomerId(
//            @PathVariable String customerId,
//            @RequestHeader("Authorization") String token) {
//        log.info("Get accounts by customer ID request: {}", customerId);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String tokenCustomerId = jwtUtil.extractCustomerId(jwt);
//
//        // If customer, verify they're requesting their own accounts
//        if ("CUSTOMER".equals(role) && !customerId.equals(tokenCustomerId)) {
//            return ResponseEntity
//                    .status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.error("Access denied: You can only view your own accounts"));
//        }
//
//        List<AccountListItemDTO> accounts = accountService.getAccountsByCustomerId(customerId);
//        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
//    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<AccountListItemDTO>>> getAccountsByStatus(
            @PathVariable String status) {
        log.info("Get accounts by status request: {}", status);
        List<AccountListItemDTO> accounts = accountService.getAccountsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
    }

//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<AccountResponseDTO>> updateAccount(
//            @PathVariable Long id,
//            @Valid @RequestBody AccountUpdateRequestDTO request,
//            @RequestHeader("Authorization") String token) {
//        log.info("Update account request for ID: {}", id);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        // Get account first to check ownership
//        AccountResponseDTO existingAccount = accountService.getAccountById(id);
//
//        // If customer, verify ownership
//        if ("CUSTOMER".equals(role)) {
//            if (!existingAccount.getCustomerId().equals(customerId)) {
//                return ResponseEntity
//                        .status(HttpStatus.FORBIDDEN)
//                        .body(ApiResponse.error("Access denied: You can only update your own accounts"));
//            }
//            // Customers cannot change branch
//            request.setBranchCode(null);
//        }
//
//        AccountResponseDTO response = accountService.updateAccount(id, request);
//        return ResponseEntity.ok(ApiResponse.success("Account updated successfully", response));
//    }

//    @PatchMapping("/{accountNumber}/freeze")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
//    public ResponseEntity<ApiResponse<AccountResponseDTO>> freezeAccount(
//            @PathVariable String accountNumber) {
//        log.info("Freeze account request: {}", accountNumber);
//        AccountResponseDTO response = accountService.freezeAccount(accountNumber);
//        return ResponseEntity.ok(ApiResponse.success("Account frozen successfully", response));
//    }
//
//    @PatchMapping("/{accountNumber}/unfreeze")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
//    public ResponseEntity<ApiResponse<AccountResponseDTO>> unfreezeAccount(
//            @PathVariable String accountNumber) {
//        log.info("Unfreeze account request: {}", accountNumber);
//        AccountResponseDTO response = accountService.unfreezeAccount(accountNumber);
//        return ResponseEntity.ok(ApiResponse.success("Account unfrozen successfully", response));
//    }

//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
//    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
//        log.info("Close account request for ID: {}", id);
//        accountService.deleteAccount(id);
//        return ResponseEntity.ok(ApiResponse.success("Account closed successfully", null));
//    }
//
//    @PostMapping("/statement")
//    public ResponseEntity<ApiResponse<AccountStatementDTO>> getAccountStatement(
//            @Valid @RequestBody AccountStatementRequestDTO request,
//            @RequestHeader("Authorization") String token) {
//        log.info("Account statement request for: {}", request.getAccountNumber());
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role)) {
//            AccountResponseDTO account = accountService.getAccountByAccountNumber(request.getAccountNumber());
//            if (!account.getCustomerId().equals(customerId)) {
//                return ResponseEntity
//                        .status(HttpStatus.FORBIDDEN)
//                        .body(ApiResponse.error("Access denied: You can only view your own account statements"));
//            }
//        }
//
//        AccountStatementDTO statement = accountService.getAccountStatement(request);
//        return ResponseEntity.ok(ApiResponse.success("Account statement generated successfully", statement));
//    }





























    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> getAccountById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        log.info("Get account by ID request: {}", id);

        String jwt = token.substring(7); // Extract JWT from "Bearer " prefix
        AccountResponseDTO account = accountService.getAccountById(id, jwt);

        return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully", account));
    }

    @GetMapping("/account-number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> getAccountByAccountNumber(
            @PathVariable String accountNumber,
            @RequestHeader("Authorization") String token) {
        log.info("Get account by account number request: {}", accountNumber);

        String jwt = token.substring(7);
        AccountResponseDTO account = accountService.getAccountByAccountNumber(accountNumber, jwt);

        return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully", account));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<AccountListItemDTO>>> getAccountsByCustomerId(
            @PathVariable String customerId,
            @RequestHeader("Authorization") String token) {
        log.info("Get accounts by customer ID request: {}", customerId);

        String jwt = token.substring(7);
        List<AccountListItemDTO> accounts = accountService.getAccountsByCustomerId(customerId, jwt);

        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountUpdateRequestDTO request,
            @RequestHeader("Authorization") String token) {
        log.info("Update account request for ID: {}", id);

        String jwt = token.substring(7);
        AccountResponseDTO response = accountService.updateAccount(id, request, jwt);

        return ResponseEntity.ok(ApiResponse.success("Account updated successfully", response));
    }

    @PatchMapping("/{accountNumber}/freeze")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> freezeAccount(
            @PathVariable String accountNumber,
            @RequestHeader("Authorization") String token) {
        log.info("Freeze account request: {}", accountNumber);

        String jwt = token.substring(7);
        AccountResponseDTO response = accountService.freezeAccount(accountNumber, jwt);

        return ResponseEntity.ok(ApiResponse.success("Account frozen successfully", response));
    }

    @PatchMapping("/{accountNumber}/unfreeze")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> unfreezeAccount(
            @PathVariable String accountNumber,
            @RequestHeader("Authorization") String token) {
        log.info("Unfreeze account request: {}", accountNumber);

        String jwt = token.substring(7);
        AccountResponseDTO response = accountService.unfreezeAccount(accountNumber, jwt);

        return ResponseEntity.ok(ApiResponse.success("Account unfrozen successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        log.info("Close account request for ID: {}", id);

        String jwt = token.substring(7);
        accountService.deleteAccount(id, jwt);

        return ResponseEntity.ok(ApiResponse.success("Account closed successfully", null));
    }

    @PostMapping("/statement")
    public ResponseEntity<ApiResponse<AccountStatementDTO>> getAccountStatement(
            @Valid @RequestBody AccountStatementRequestDTO request,
            @RequestHeader("Authorization") String token) {
        log.info("Account statement request for: {}", request.getAccountNumber());

        String jwt = token.substring(7);
        AccountStatementDTO statement = accountService.getAccountStatement(request, jwt);

        return ResponseEntity.ok(ApiResponse.success("Account statement generated successfully", statement));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<AccountListItemDTO>>> getAllAccounts(
            @RequestHeader("Authorization") String token) {
        log.info("Get all accounts request");

        String jwt = token.substring(7);
        List<AccountListItemDTO> accounts = accountService.getAllAccounts(jwt);

        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accounts));
    }

}





//package com.izak.demoBankManagement.controller;
//
//
//import com.izak.demoBankManagement.dto.*;
//import com.izak.demoBankManagement.service.AccountService;
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
//@RequestMapping("/api/accounts")
//@RequiredArgsConstructor
//@Validated
//@Slf4j
//@CrossOrigin(origins = "http://localhost:4200")
//public class AccountController {
//
//    private final AccountService accountService;
//
//    // ============================================
//    // CREATE ACCOUNT
//    // POST /api/accounts
//    // ============================================
//    @PostMapping
//    public ResponseEntity<ApiResponse<AccountResponseDTO>> createAccount(
//            @Valid @RequestBody AccountCreateRequestDTO request) {
//
//        log.info("Create account request received for customer: {}", request.getCustomerId());
//
//        AccountResponseDTO response = accountService.createAccount(request);
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Account created successfully", response));
//    }
//
//    // ============================================
//    // GET ALL ACCOUNTS
//    // GET /api/accounts
//    // ============================================
//    @GetMapping
//    public ResponseEntity<ApiResponse<List<AccountListItemDTO>>> getAllAccounts() {
//        log.info("Get all accounts request");
//
//        List<AccountListItemDTO> accounts = accountService.getAllAccounts();
//        return ResponseEntity
//                .ok(ApiResponse.success("Accounts retrieved successfully", accounts));
//    }
//
//    // ============================================
//    // GET ACCOUNT BY ID
//    // GET /api/accounts/{id}
//    // ============================================
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<AccountResponseDTO>> getAccountById(@PathVariable Long id) {
//        log.info("Get account by ID request: {}", id);
//
//        AccountResponseDTO account = accountService.getAccountById(id);
//        return ResponseEntity
//                .ok(ApiResponse.success("Account retrieved successfully", account));
//    }
//
//    // ============================================
//    // GET ACCOUNT BY ACCOUNT NUMBER
//    // GET /api/accounts/account-number/{accountNumber}
//    // ============================================
//    @GetMapping("/account-number/{accountNumber}")
//    public ResponseEntity<ApiResponse<AccountResponseDTO>> getAccountByAccountNumber(
//            @PathVariable String accountNumber) {
//        log.info("Get account by account number request: {}", accountNumber);
//
//        AccountResponseDTO account = accountService.getAccountByAccountNumber(accountNumber);
//        return ResponseEntity
//                .ok(ApiResponse.success("Account retrieved successfully", account));
//    }
//
//    // ============================================
//    // GET ACCOUNTS BY CUSTOMER ID
//    // GET /api/accounts/customer/{customerId}
//    // ============================================
//    @GetMapping("/customer/{customerId}")
//    public ResponseEntity<ApiResponse<List<AccountListItemDTO>>> getAccountsByCustomerId(
//            @PathVariable String customerId) {
//        log.info("Get accounts by customer ID request: {}", customerId);
//
//        List<AccountListItemDTO> accounts = accountService.getAccountsByCustomerId(customerId);
//        return ResponseEntity
//                .ok(ApiResponse.success("Accounts retrieved successfully", accounts));
//    }
//
//    // ============================================
//    // GET ACCOUNTS BY STATUS
//    // GET /api/accounts/status/{status}
//    // ============================================
//    @GetMapping("/status/{status}")
//    public ResponseEntity<ApiResponse<List<AccountListItemDTO>>> getAccountsByStatus(
//            @PathVariable String status) {
//        log.info("Get accounts by status request: {}", status);
//
//        List<AccountListItemDTO> accounts = accountService.getAccountsByStatus(status);
//        return ResponseEntity
//                .ok(ApiResponse.success("Accounts retrieved successfully", accounts));
//    }
//
//    // ============================================
//    // UPDATE ACCOUNT
//    // PUT /api/accounts/{id}
//    // ============================================
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<AccountResponseDTO>> updateAccount(
//            @PathVariable Long id,
//            @Valid @RequestBody AccountUpdateRequestDTO request) {
//
//        log.info("Update account request for ID: {}", id);
//
//        AccountResponseDTO response = accountService.updateAccount(id, request);
//        return ResponseEntity
//                .ok(ApiResponse.success("Account updated successfully", response));
//    }
//
//    // ============================================
//    // FREEZE ACCOUNT
//    // PATCH /api/accounts/{accountNumber}/freeze
//    // ============================================
//    @PatchMapping("/{accountNumber}/freeze")
//    public ResponseEntity<ApiResponse<AccountResponseDTO>> freezeAccount(
//            @PathVariable String accountNumber) {
//
//        log.info("Freeze account request: {}", accountNumber);
//
//        AccountResponseDTO response = accountService.freezeAccount(accountNumber);
//        return ResponseEntity
//                .ok(ApiResponse.success("Account frozen successfully", response));
//    }
//
//    // ============================================
//    // UNFREEZE ACCOUNT
//    // PATCH /api/accounts/{accountNumber}/unfreeze
//    // ============================================
//    @PatchMapping("/{accountNumber}/unfreeze")
//    public ResponseEntity<ApiResponse<AccountResponseDTO>> unfreezeAccount(
//            @PathVariable String accountNumber) {
//
//        log.info("Unfreeze account request: {}", accountNumber);
//
//        AccountResponseDTO response = accountService.unfreezeAccount(accountNumber);
//        return ResponseEntity
//                .ok(ApiResponse.success("Account unfrozen successfully", response));
//    }
//
//    // ============================================
//    // DELETE ACCOUNT (Close Account)
//    // DELETE /api/accounts/{id}
//    // ============================================
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
//        log.info("Close account request for ID: {}", id);
//
//        accountService.deleteAccount(id);
//        return ResponseEntity
//                .ok(ApiResponse.success("Account closed successfully", null));
//    }
//
//    // ============================================
//    // GET ACCOUNT STATEMENT
//    // POST /api/accounts/statement
//    // ============================================
//    @PostMapping("/statement")
//    public ResponseEntity<ApiResponse<AccountStatementDTO>> getAccountStatement(
//            @Valid @RequestBody AccountStatementRequestDTO request) {
//
//        log.info("Account statement request for: {} from {} to {}",
//                request.getAccountNumber(),
//                request.getStartDate(),
//                request.getEndDate());
//
//        AccountStatementDTO statement = accountService.getAccountStatement(request);
//        return ResponseEntity
//                .ok(ApiResponse.success("Account statement generated successfully", statement));
//    }
//}

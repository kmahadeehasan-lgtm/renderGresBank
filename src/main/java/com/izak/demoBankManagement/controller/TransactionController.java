package com.izak.demoBankManagement.controller;

import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> transferFunds(
            @Valid @RequestBody TransferRequestDTO request,
            @RequestHeader("Authorization") String token) {

        log.info("Transfer request received: {} -> {}",
                request.getFromAccountNumber(), request.getToAccountNumber());

        TransactionResponseDTO response = transactionService.transferFunds(request, token);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fund transfer completed successfully", response));
    }

    @PostMapping("/deposit")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> depositMoney(
            @Valid @RequestBody DepositRequestDTO request,
            @RequestHeader("Authorization") String token) {

        log.info("Deposit request received: Account {}", request.getAccountNumber());

        TransactionResponseDTO response = transactionService.depositMoney(request, token);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deposit completed successfully", response));
    }

    @PostMapping("/withdraw")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> withdrawMoney(
            @Valid @RequestBody WithdrawRequestDTO request,
            @RequestHeader("Authorization") String token) {

        log.info("Withdrawal request received: Account {}", request.getAccountNumber());

        TransactionResponseDTO response = transactionService.withdrawMoney(request, token);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal completed successfully", response));
    }

    @GetMapping("/balance/{accountNumber}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<AccountBalanceDTO>> getAccountBalance(
            @PathVariable String accountNumber,
            @RequestHeader("Authorization") String token) {

        log.info("Balance inquiry for account: {}", accountNumber);

        AccountBalanceDTO balance = transactionService.getAccountBalance(accountNumber, token);

        return ResponseEntity.ok(ApiResponse.success("Balance retrieved successfully", balance));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Transaction service is running", "OK"));
    }
}



//package com.izak.demoBankManagement.controller;
//
//import com.izak.demoBankManagement.dto.*;
//import com.izak.demoBankManagement.service.TransactionService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//import jakarta.validation.Valid;
//
//@RestController
//@RequestMapping("/api/transactions")
//@RequiredArgsConstructor
//@Validated
//@Slf4j
//@CrossOrigin(origins = "http://localhost:4200")
//public class TransactionController {
//
//    private final TransactionService transactionService;
//
//    @PostMapping("/transfer")
//    public ResponseEntity<ApiResponse<TransactionResponseDTO>> transferFunds(
//            @Valid @RequestBody TransferRequestDTO request,
//            @RequestHeader("Authorization") String token) {
//
//        log.info("Transfer request received: {} -> {}",
//                request.getFromAccountNumber(), request.getToAccountNumber());
//
//        TransactionResponseDTO response = transactionService.transferFunds(request, token);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Fund transfer completed successfully", response));
//    }
//
//    @PostMapping("/deposit")
//    public ResponseEntity<ApiResponse<TransactionResponseDTO>> depositMoney(
//            @Valid @RequestBody DepositRequestDTO request,
//            @RequestHeader("Authorization") String token) {
//
//        log.info("Deposit request received: Account {}", request.getAccountNumber());
//
//        TransactionResponseDTO response = transactionService.depositMoney(request, token);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Deposit completed successfully", response));
//    }
//
//    @PostMapping("/withdraw")
//    public ResponseEntity<ApiResponse<TransactionResponseDTO>> withdrawMoney(
//            @Valid @RequestBody WithdrawRequestDTO request,
//            @RequestHeader("Authorization") String token) {
//
//        log.info("Withdrawal request received: Account {}", request.getAccountNumber());
//
//        TransactionResponseDTO response = transactionService.withdrawMoney(request, token);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Withdrawal completed successfully", response));
//    }
//
//    @GetMapping("/balance/{accountNumber}")
//    public ResponseEntity<ApiResponse<AccountBalanceDTO>> getAccountBalance(
//            @PathVariable String accountNumber,
//            @RequestHeader("Authorization") String token) {
//
//        log.info("Balance inquiry for account: {}", accountNumber);
//
//        AccountBalanceDTO balance = transactionService.getAccountBalance(accountNumber, token);
//
//        return ResponseEntity.ok(ApiResponse.success("Balance retrieved successfully", balance));
//    }
//
//    @GetMapping("/health")
//    public ResponseEntity<ApiResponse<String>> healthCheck() {
//        return ResponseEntity.ok(ApiResponse.success("Transaction service is running", "OK"));
//    }
//}
//
//
//
//
//
//
//
//
//
//
////package com.izak.demoBankManagement.controller;
////
////import com.izak.demoBankManagement.dto.*;
////import com.izak.demoBankManagement.service.TransactionService;
////import com.izak.demoBankManagement.service.AccountService;
////import com.izak.demoBankManagement.security.JwtUtil;
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.http.HttpStatus;
////import org.springframework.http.ResponseEntity;
////import org.springframework.validation.annotation.Validated;
////import org.springframework.web.bind.annotation.*;
////
////import jakarta.validation.Valid;
////
////@RestController
////@RequestMapping("/api/transactions")
////@RequiredArgsConstructor
////@Validated
////@Slf4j
////@CrossOrigin(origins = "http://localhost:4200")
////public class TransactionController {
////
////    private final TransactionService transactionService;
////    private final AccountService accountService;
////    private final JwtUtil jwtUtil;
////
////    @PostMapping("/transfer")
////    public ResponseEntity<ApiResponse<TransactionResponseDTO>> transferFunds(
////            @Valid @RequestBody TransferRequestDTO request,
////            @RequestHeader("Authorization") String token) {
////        log.info("Transfer request received: {} -> {}",
////                request.getFromAccountNumber(), request.getToAccountNumber());
////
////        String jwt = token.substring(7);
////        String role = jwtUtil.extractRole(jwt);
////        String customerId = jwtUtil.extractCustomerId(jwt);
////
////        // Verify ownership for customers
////        if ("CUSTOMER".equals(role)) {
////            AccountResponseDTO fromAccount = accountService.getAccountByAccountNumber(request.getFromAccountNumber());
////            if (!fromAccount.getCustomerId().equals(customerId)) {
////                return ResponseEntity
////                        .status(HttpStatus.FORBIDDEN)
////                        .body(ApiResponse.error("Access denied: You can only transfer from your own accounts"));
////            }
////        }
////
////        TransactionResponseDTO response = transactionService.transferFunds(request);
////        return ResponseEntity
////                .status(HttpStatus.CREATED)
////                .body(ApiResponse.success("Fund transfer completed successfully", response));
////    }
////
////    @PostMapping("/deposit")
////    public ResponseEntity<ApiResponse<TransactionResponseDTO>> depositMoney(
////            @Valid @RequestBody DepositRequestDTO request,
////            @RequestHeader("Authorization") String token) {
////        log.info("Deposit request received: Account {}", request.getAccountNumber());
////
////        String jwt = token.substring(7);
////        String role = jwtUtil.extractRole(jwt);
////        String customerId = jwtUtil.extractCustomerId(jwt);
////
////        // Verify ownership for customers
////        if ("CUSTOMER".equals(role)) {
////            AccountResponseDTO account = accountService.getAccountByAccountNumber(request.getAccountNumber());
////            if (!account.getCustomerId().equals(customerId)) {
////                return ResponseEntity
////                        .status(HttpStatus.FORBIDDEN)
////                        .body(ApiResponse.error("Access denied: You can only deposit to your own accounts"));
////            }
////        }
////
////        TransactionResponseDTO response = transactionService.depositMoney(request);
////        return ResponseEntity
////                .status(HttpStatus.CREATED)
////                .body(ApiResponse.success("Deposit completed successfully", response));
////    }
////
////    @PostMapping("/withdraw")
////    public ResponseEntity<ApiResponse<TransactionResponseDTO>> withdrawMoney(
////            @Valid @RequestBody WithdrawRequestDTO request,
////            @RequestHeader("Authorization") String token) {
////        log.info("Withdrawal request received: Account {}", request.getAccountNumber());
////
////        String jwt = token.substring(7);
////        String role = jwtUtil.extractRole(jwt);
////        String customerId = jwtUtil.extractCustomerId(jwt);
////
////        // Verify ownership for customers
////        if ("CUSTOMER".equals(role)) {
////            AccountResponseDTO account = accountService.getAccountByAccountNumber(request.getAccountNumber());
////            if (!account.getCustomerId().equals(customerId)) {
////                return ResponseEntity
////                        .status(HttpStatus.FORBIDDEN)
////                        .body(ApiResponse.error("Access denied: You can only withdraw from your own accounts"));
////            }
////        }
////
////        TransactionResponseDTO response = transactionService.withdrawMoney(request);
////        return ResponseEntity
////                .status(HttpStatus.CREATED)
////                .body(ApiResponse.success("Withdrawal completed successfully", response));
////    }
////
////    @GetMapping("/balance/{accountNumber}")
////    public ResponseEntity<ApiResponse<AccountBalanceDTO>> getAccountBalance(
////            @PathVariable String accountNumber,
////            @RequestHeader("Authorization") String token) {
////        log.info("Balance inquiry for account: {}", accountNumber);
////
////        String jwt = token.substring(7);
////        String role = jwtUtil.extractRole(jwt);
////        String customerId = jwtUtil.extractCustomerId(jwt);
////
////        AccountBalanceDTO balance = transactionService.getAccountBalance(accountNumber);
////
////        // Verify ownership for customers
////        if ("CUSTOMER".equals(role) && !balance.getCustomerId().equals(customerId)) {
////            return ResponseEntity
////                    .status(HttpStatus.FORBIDDEN)
////                    .body(ApiResponse.error("Access denied: You can only view your own account balance"));
////        }
////
////        return ResponseEntity.ok(ApiResponse.success("Balance retrieved successfully", balance));
////    }
////
////    @GetMapping("/health")
////    public ResponseEntity<ApiResponse<String>> healthCheck() {
////        return ResponseEntity.ok(ApiResponse.success("Transaction service is running", "OK"));
////    }
////}
////
////
//////package com.izak.demoBankManagement.controller;
////
//////
//////import com.izak.demoBankManagement.dto.*;
//////import com.izak.demoBankManagement.service.TransactionService;
//////import lombok.RequiredArgsConstructor;
//////import lombok.extern.slf4j.Slf4j;
//////import org.springframework.http.HttpStatus;
//////import org.springframework.http.ResponseEntity;
//////import org.springframework.validation.annotation.Validated;
//////import org.springframework.web.bind.annotation.*;
//////
//////import jakarta.validation.Valid;
//////
//////@RestController
//////@RequestMapping("/api/transactions")
//////@RequiredArgsConstructor
//////@Validated
//////@Slf4j
//////@CrossOrigin(origins = "http://localhost:4200") // Allow Angular app
//////public class TransactionController {
//////
//////    private final TransactionService transactionService;
//////
//////
//////    @PostMapping("/transfer")
//////    public ResponseEntity<ApiResponse<TransactionResponseDTO>> transferFunds(
//////            @Valid @RequestBody TransferRequestDTO request) {
//////
//////        log.info("Transfer request received: {} -> {}, Amount: {}",
//////                request.getFromAccountNumber(),
//////                request.getToAccountNumber(),
//////                request.getAmount());
//////
//////        try {
//////            TransactionResponseDTO response = transactionService.transferFunds(request);
//////            return ResponseEntity
//////                    .status(HttpStatus.CREATED)
//////                    .body(ApiResponse.success("Fund transfer completed successfully", response));
//////        } catch (Exception ex) {
//////            log.error("Transfer failed: {}", ex.getMessage());
//////            throw ex; // Let GlobalExceptionHandler handle it
//////        }
//////    }
//////
//////
//////    @PostMapping("/deposit")
//////    public ResponseEntity<ApiResponse<TransactionResponseDTO>> depositMoney(
//////            @Valid @RequestBody DepositRequestDTO request) {
//////
//////        log.info("Deposit request received: Account {}, Amount: {}",
//////                request.getAccountNumber(),
//////                request.getAmount());
//////
//////        try {
//////            TransactionResponseDTO response = transactionService.depositMoney(request);
//////            return ResponseEntity
//////                    .status(HttpStatus.CREATED)
//////                    .body(ApiResponse.success("Deposit completed successfully", response));
//////        } catch (Exception ex) {
//////            log.error("Deposit failed: {}", ex.getMessage());
//////            throw ex;
//////        }
//////    }
//////
//////
//////    @PostMapping("/withdraw")
//////    public ResponseEntity<ApiResponse<TransactionResponseDTO>> withdrawMoney(
//////            @Valid @RequestBody WithdrawRequestDTO request) {
//////
//////        log.info("Withdrawal request received: Account {}, Amount: {}",
//////                request.getAccountNumber(),
//////                request.getAmount());
//////
//////        try {
//////            TransactionResponseDTO response = transactionService.withdrawMoney(request);
//////            return ResponseEntity
//////                    .status(HttpStatus.CREATED)
//////                    .body(ApiResponse.success("Withdrawal completed successfully", response));
//////        } catch (Exception ex) {
//////            log.error("Withdrawal failed: {}", ex.getMessage());
//////            throw ex;
//////        }
//////    }
//////
//////
//////    @GetMapping("/balance/{accountNumber}")
//////    public ResponseEntity<ApiResponse<AccountBalanceDTO>> getAccountBalance(
//////            @PathVariable String accountNumber) {
//////
//////        log.info("Balance inquiry for account: {}", accountNumber);
//////
//////        AccountBalanceDTO balance = transactionService.getAccountBalance(accountNumber);
//////        return ResponseEntity
//////                .ok(ApiResponse.success("Balance retrieved successfully", balance));
//////    }
//////
//////
//////    @GetMapping("/health")
//////    public ResponseEntity<ApiResponse<String>> healthCheck() {
//////        return ResponseEntity.ok(ApiResponse.success("Transaction service is running", "OK"));
//////    }
//////}

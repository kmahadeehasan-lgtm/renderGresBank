package com.izak.demoBankManagement.controller;

import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.exception.DPSMaturityCalculationDTO;
import com.izak.demoBankManagement.service.DPSService;
import com.izak.demoBankManagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/dps")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class DPSController {

    private final DPSService dpsService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<DPSResponseDTO>> createDPS(
            @Valid @RequestBody DPSCreateRequestDTO request,
            @RequestHeader("Authorization") String token) {
        log.info("Create DPS request for customer: {}", request.getCustomerId());

        String jwt = token.substring(7);
        DPSResponseDTO response = dpsService.createDPS(request, jwt);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("DPS account created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getAllDPS(
            @RequestHeader("Authorization") String token) {
        log.info("Get all DPS accounts request");

        String jwt = token.substring(7);
        List<DPSResponseDTO> dpsAccounts = dpsService.getAllDPS(jwt);

        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<DPSResponseDTO>> getDPSById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        log.info("Get DPS by ID: {}", id);

        String jwt = token.substring(7);
        DPSResponseDTO dps = dpsService.getDPSById(id, jwt);

        return ResponseEntity.ok(ApiResponse.success("DPS retrieved successfully", dps));
    }

    @GetMapping("/number/{dpsNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<DPSResponseDTO>> getDPSByNumber(
            @PathVariable String dpsNumber,
            @RequestHeader("Authorization") String token) {
        log.info("Get DPS by number: {}", dpsNumber);

        String jwt = token.substring(7);
        DPSResponseDTO dps = dpsService.getDPSByNumber(dpsNumber, jwt);

        return ResponseEntity.ok(ApiResponse.success("DPS retrieved successfully", dps));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByCustomerId(
            @PathVariable String customerId,
            @RequestHeader("Authorization") String token) {
        log.info("Get DPS accounts for customer: {}", customerId);

        String jwt = token.substring(7);
        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByCustomerId(customerId, jwt);

        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByBranch(
            @PathVariable Long branchId,
            @RequestHeader("Authorization") String token) {
        log.info("Get DPS accounts for branch: {}", branchId);

        String jwt = token.substring(7);
        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByBranch(branchId, jwt);

        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByStatus(
            @PathVariable String status,
            @RequestHeader("Authorization") String token) {
        log.info("Get DPS accounts by status: {}", status);

        String jwt = token.substring(7);
        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByStatus(status, jwt);

        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
    }

    @PostMapping("/pay-installment")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> payInstallment(
            @Valid @RequestBody DPSPaymentRequestDTO request,
            @RequestHeader("Authorization") String token) {
        log.info("DPS payment request for: {}", request.getDpsNumber());

        String jwt = token.substring(7);
        TransactionResponseDTO response = dpsService.payInstallment(request, jwt);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("DPS installment paid successfully", response));
    }

    @GetMapping("/statement/{dpsNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<DPSStatementDTO>> getDPSStatement(
            @PathVariable String dpsNumber,
            @RequestHeader("Authorization") String token) {
        log.info("DPS statement request for: {}", dpsNumber);

        String jwt = token.substring(7);
        DPSStatementDTO statement = dpsService.getDPSStatement(dpsNumber, jwt);

        return ResponseEntity.ok(ApiResponse.success("DPS statement generated successfully", statement));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<DPSResponseDTO>> updateDPS(
            @PathVariable Long id,
            @Valid @RequestBody DPSUpdateRequestDTO request,
            @RequestHeader("Authorization") String token) {
        log.info("Update DPS request for ID: {}", id);

        String jwt = token.substring(7);
        DPSResponseDTO response = dpsService.updateDPS(id, request, jwt);

        return ResponseEntity.ok(ApiResponse.success("DPS updated successfully", response));
    }

    @PatchMapping("/{dpsNumber}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<DPSResponseDTO>> closeDPS(
            @PathVariable String dpsNumber,
            @RequestParam(required = false) String reason,
            @RequestHeader("Authorization") String token) {
        log.info("Close DPS request for: {}", dpsNumber);

        String jwt = token.substring(7);
        DPSResponseDTO response = dpsService.closeDPS(dpsNumber, reason, jwt);

        return ResponseEntity.ok(ApiResponse.success("DPS closed successfully", response));
    }

    @PatchMapping("/{dpsNumber}/mature")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<DPSResponseDTO>> matureDPS(
            @PathVariable String dpsNumber,
            @RequestHeader("Authorization") String token) {
        log.info("Mature DPS request for: {}", dpsNumber);

        String jwt = token.substring(7);
        DPSResponseDTO response = dpsService.matureDPS(dpsNumber, jwt);

        return ResponseEntity.ok(ApiResponse.success("DPS matured successfully", response));
    }

    @GetMapping("/calculate-maturity")
    public ResponseEntity<ApiResponse<DPSMaturityCalculationDTO>> calculateMaturity(
            @RequestParam BigDecimal monthlyInstallment,
            @RequestParam Integer tenureMonths,
            @RequestParam BigDecimal interestRate) {
        log.info("Calculate DPS maturity for installment: {}, tenure: {}", monthlyInstallment, tenureMonths);

        DPSMaturityCalculationDTO calculation = dpsService.calculateMaturity(
                monthlyInstallment, tenureMonths, interestRate);

        return ResponseEntity.ok(ApiResponse.success("Maturity calculated successfully", calculation));
    }
}









//package com.izak.demoBankManagement.controller;
//
//import com.izak.demoBankManagement.dto.*;
//import com.izak.demoBankManagement.exception.DPSMaturityCalculationDTO;
//import com.izak.demoBankManagement.service.DPSService;
//import com.izak.demoBankManagement.security.JwtUtil;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//import jakarta.validation.Valid;
//import java.math.BigDecimal;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/dps")
//@RequiredArgsConstructor
//@Validated
//@Slf4j
//@CrossOrigin(origins = "http://localhost:4200")
//public class DPSController {
//
//    private final DPSService dpsService;
//    private final JwtUtil jwtUtil;
//
//    @PostMapping
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<DPSResponseDTO>> createDPS(
//            @Valid @RequestBody DPSCreateRequestDTO request,
//            @RequestHeader("Authorization") String token) {
//        log.info("Create DPS request for customer: {}", request.getCustomerId());
//
//        String jwt = token.substring(7);
//        DPSResponseDTO response = dpsService.createDPS(request, jwt);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("DPS account created successfully", response));
//    }
//
//    @GetMapping
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
//    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getAllDPS(
//            @RequestHeader("Authorization") String token) {
//        log.info("Get all DPS accounts request");
//
//        String jwt = token.substring(7);
//        List<DPSResponseDTO> dpsAccounts = dpsService.getAllDPS(jwt);
//
//        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
//    }
//
//    @GetMapping("/{id}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<DPSResponseDTO>> getDPSById(
//            @PathVariable Long id,
//            @RequestHeader("Authorization") String token) {
//        log.info("Get DPS by ID: {}", id);
//
//        String jwt = token.substring(7);
//        DPSResponseDTO dps = dpsService.getDPSById(id, jwt);
//
//        return ResponseEntity.ok(ApiResponse.success("DPS retrieved successfully", dps));
//    }
//
//    @GetMapping("/number/{dpsNumber}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<DPSResponseDTO>> getDPSByNumber(
//            @PathVariable String dpsNumber,
//            @RequestHeader("Authorization") String token) {
//        log.info("Get DPS by number: {}", dpsNumber);
//
//        String jwt = token.substring(7);
//        DPSResponseDTO dps = dpsService.getDPSByNumber(dpsNumber, jwt);
//
//        return ResponseEntity.ok(ApiResponse.success("DPS retrieved successfully", dps));
//    }
//
//    @GetMapping("/customer/{customerId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByCustomerId(
//            @PathVariable String customerId,
//            @RequestHeader("Authorization") String token) {
//        log.info("Get DPS accounts for customer: {}", customerId);
//
//        String jwt = token.substring(7);
//        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByCustomerId(customerId, jwt);
//
//        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
//    }
//
//    @GetMapping("/branch/{branchId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
//    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByBranch(
//            @PathVariable Long branchId,
//            @RequestHeader("Authorization") String token) {
//        log.info("Get DPS accounts for branch: {}", branchId);
//
//        String jwt = token.substring(7);
//        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByBranch(branchId, jwt);
//
//        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
//    }
//
//    @GetMapping("/status/{status}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
//    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByStatus(
//            @PathVariable String status,
//            @RequestHeader("Authorization") String token) {
//        log.info("Get DPS accounts by status: {}", status);
//
//        String jwt = token.substring(7);
//        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByStatus(status, jwt);
//
//        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
//    }
//
//    @PostMapping("/pay-installment")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<TransactionResponseDTO>> payInstallment(
//            @Valid @RequestBody DPSPaymentRequestDTO request,
//            @RequestHeader("Authorization") String token) {
//        log.info("DPS payment request for: {}", request.getDpsNumber());
//
//        String jwt = token.substring(7);
//        TransactionResponseDTO response = dpsService.payInstallment(request, jwt);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("DPS installment paid successfully", response));
//    }
//
//    @GetMapping("/statement/{dpsNumber}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<DPSStatementDTO>> getDPSStatement(
//            @PathVariable String dpsNumber,
//            @RequestHeader("Authorization") String token) {
//        log.info("DPS statement request for: {}", dpsNumber);
//
//        String jwt = token.substring(7);
//        DPSStatementDTO statement = dpsService.getDPSStatement(dpsNumber, jwt);
//
//        return ResponseEntity.ok(ApiResponse.success("DPS statement generated successfully", statement));
//    }
//
//    @PutMapping("/{id}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<DPSResponseDTO>> updateDPS(
//            @PathVariable Long id,
//            @Valid @RequestBody DPSUpdateRequestDTO request,
//            @RequestHeader("Authorization") String token) {
//        log.info("Update DPS request for ID: {}", id);
//
//        String jwt = token.substring(7);
//        DPSResponseDTO response = dpsService.updateDPS(id, request, jwt);
//
//        return ResponseEntity.ok(ApiResponse.success("DPS updated successfully", response));
//    }
//
//    @PatchMapping("/{dpsNumber}/close")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<DPSResponseDTO>> closeDPS(
//            @PathVariable String dpsNumber,
//            @RequestParam(required = false) String reason,
//            @RequestHeader("Authorization") String token) {
//        log.info("Close DPS request for: {}", dpsNumber);
//
//        String jwt = token.substring(7);
//        DPSResponseDTO response = dpsService.closeDPS(dpsNumber, reason, jwt);
//
//        return ResponseEntity.ok(ApiResponse.success("DPS closed successfully", response));
//    }
//
//    @PatchMapping("/{dpsNumber}/mature")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CUSTOMER')")
//    public ResponseEntity<ApiResponse<DPSResponseDTO>> matureDPS(
//            @PathVariable String dpsNumber,
//            @RequestHeader("Authorization") String token) {
//        log.info("Mature DPS request for: {}", dpsNumber);
//
//        String jwt = token.substring(7);
//        DPSResponseDTO response = dpsService.matureDPS(dpsNumber, jwt);
//
//        return ResponseEntity.ok(ApiResponse.success("DPS matured successfully", response));
//    }
//
//    @GetMapping("/calculate-maturity")
//    public ResponseEntity<ApiResponse<DPSMaturityCalculationDTO>> calculateMaturity(
//            @RequestParam BigDecimal monthlyInstallment,
//            @RequestParam Integer tenureMonths,
//            @RequestParam BigDecimal interestRate) {
//        log.info("Calculate DPS maturity for installment: {}, tenure: {}", monthlyInstallment, tenureMonths);
//
//        DPSMaturityCalculationDTO calculation = dpsService.calculateMaturity(
//                monthlyInstallment, tenureMonths, interestRate);
//
//        return ResponseEntity.ok(ApiResponse.success("Maturity calculated successfully", calculation));
//    }
//}
//
//
//
////package com.izak.demoBankManagement.controller;
////
////import com.izak.demoBankManagement.dto.*;
////import com.izak.demoBankManagement.exception.DPSMaturityCalculationDTO;
////import com.izak.demoBankManagement.service.DPSService;
////import com.izak.demoBankManagement.security.JwtUtil;
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.http.HttpStatus;
////import org.springframework.http.ResponseEntity;
////import org.springframework.security.access.prepost.PreAuthorize;
////import org.springframework.validation.annotation.Validated;
////import org.springframework.web.bind.annotation.*;
////
////import jakarta.validation.Valid;
////import java.math.BigDecimal;
////import java.util.List;
////import java.util.stream.Collectors;
////
////@RestController
////@RequestMapping("/api/dps")
////@RequiredArgsConstructor
////@Validated
////@Slf4j
////@CrossOrigin(origins = "http://localhost:4200")
////public class DPSController {
////
////    private final DPSService dpsService;
////    private final JwtUtil jwtUtil;
////
////    @PostMapping
////    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
////    public ResponseEntity<ApiResponse<DPSResponseDTO>> createDPS(
////            @Valid @RequestBody DPSCreateRequestDTO request) {
////        log.info("Create DPS request for customer: {}", request.getCustomerId());
////        DPSResponseDTO response = dpsService.createDPS(request);
////        return ResponseEntity
////                .status(HttpStatus.CREATED)
////                .body(ApiResponse.success("DPS account created successfully", response));
////    }
////
////    @GetMapping
////    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
////    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getAllDPS() {
////        log.info("Get all DPS accounts request");
////        List<DPSResponseDTO> dpsAccounts = dpsService.getAllDPS();
////        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
////    }
////
////    @GetMapping("/{id}")
////    public ResponseEntity<ApiResponse<DPSResponseDTO>> getDPSById(
////            @PathVariable Long id,
////            @RequestHeader("Authorization") String token) {
////        log.info("Get DPS by ID: {}", id);
////
////        String jwt = token.substring(7);
////        String role = jwtUtil.extractRole(jwt);
////        String customerId = jwtUtil.extractCustomerId(jwt);
////
////        DPSResponseDTO dps = dpsService.getDPSById(id);
////
////        // Verify ownership for customers
////        if ("CUSTOMER".equals(role) && !dps.getCustomerId().equals(customerId)) {
////            return ResponseEntity
////                    .status(HttpStatus.FORBIDDEN)
////                    .body(ApiResponse.error("Access denied: You can only view your own DPS accounts"));
////        }
////
////        return ResponseEntity.ok(ApiResponse.success("DPS retrieved successfully", dps));
////    }
////
////    @GetMapping("/number/{dpsNumber}")
////    public ResponseEntity<ApiResponse<DPSResponseDTO>> getDPSByNumber(
////            @PathVariable String dpsNumber,
////            @RequestHeader("Authorization") String token) {
////        log.info("Get DPS by number: {}", dpsNumber);
////
////        String jwt = token.substring(7);
////        String role = jwtUtil.extractRole(jwt);
////        String customerId = jwtUtil.extractCustomerId(jwt);
////
////        DPSResponseDTO dps = dpsService.getDPSByNumber(dpsNumber);
////
////        // Verify ownership for customers
////        if ("CUSTOMER".equals(role) && !dps.getCustomerId().equals(customerId)) {
////            return ResponseEntity
////                    .status(HttpStatus.FORBIDDEN)
////                    .body(ApiResponse.error("Access denied: You can only view your own DPS accounts"));
////        }
////
////        return ResponseEntity.ok(ApiResponse.success("DPS retrieved successfully", dps));
////    }
////
////    @GetMapping("/customer/{customerId}")
////    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByCustomerId(
////            @PathVariable String customerId,
////            @RequestHeader("Authorization") String token) {
////        log.info("Get DPS accounts for customer: {}", customerId);
////
////        String jwt = token.substring(7);
////        String role = jwtUtil.extractRole(jwt);
////        String tokenCustomerId = jwtUtil.extractCustomerId(jwt);
////
////        // Verify ownership for customers
////        if ("CUSTOMER".equals(role) && !customerId.equals(tokenCustomerId)) {
////            return ResponseEntity
////                    .status(HttpStatus.FORBIDDEN)
////                    .body(ApiResponse.error("Access denied: You can only view your own DPS accounts"));
////        }
////
////        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByCustomerId(customerId);
////        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
////    }
////
////    @GetMapping("/branch/{branchId}")
////    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
////    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByBranch(@PathVariable Long branchId) {
////        log.info("Get DPS accounts for branch: {}", branchId);
////        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByBranch(branchId);
////        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
////    }
////
////    @GetMapping("/status/{status}")
////    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
////    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByStatus(@PathVariable String status) {
////        log.info("Get DPS accounts by status: {}", status);
////        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByStatus(status);
////        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
////    }
////
////    @PostMapping("/pay-installment")
////    public ResponseEntity<ApiResponse<TransactionResponseDTO>> payInstallment(
////            @Valid @RequestBody DPSPaymentRequestDTO request,
////            @RequestHeader("Authorization") String token) {
////        log.info("DPS payment request for: {}", request.getDpsNumber());
////
////        String jwt = token.substring(7);
////        String role = jwtUtil.extractRole(jwt);
////        String customerId = jwtUtil.extractCustomerId(jwt);
////
////        // Verify ownership for customers
////        if ("CUSTOMER".equals(role)) {
////            DPSResponseDTO dps = dpsService.getDPSByNumber(request.getDpsNumber());
////            if (!dps.getCustomerId().equals(customerId)) {
////                return ResponseEntity
////                        .status(HttpStatus.FORBIDDEN)
////                        .body(ApiResponse.error("Access denied: You can only pay for your own DPS installments"));
////            }
////        }
////
////        TransactionResponseDTO response = dpsService.payInstallment(request);
////        return ResponseEntity
////                .status(HttpStatus.CREATED)
////                .body(ApiResponse.success("DPS installment paid successfully", response));
////    }
////
////    @GetMapping("/statement/{dpsNumber}")
////    public ResponseEntity<ApiResponse<DPSStatementDTO>> getDPSStatement(
////            @PathVariable String dpsNumber,
////            @RequestHeader("Authorization") String token) {
////        log.info("DPS statement request for: {}", dpsNumber);
////
////        String jwt = token.substring(7);
////        String role = jwtUtil.extractRole(jwt);
////        String customerId = jwtUtil.extractCustomerId(jwt);
////
////        // Verify ownership for customers
////        if ("CUSTOMER".equals(role)) {
////            DPSResponseDTO dps = dpsService.getDPSByNumber(dpsNumber);
////            if (!dps.getCustomerId().equals(customerId)) {
////                return ResponseEntity
////                        .status(HttpStatus.FORBIDDEN)
////                        .body(ApiResponse.error("Access denied: You can only view your own DPS statements"));
////            }
////        }
////
////        DPSStatementDTO statement = dpsService.getDPSStatement(dpsNumber);
////        return ResponseEntity.ok(ApiResponse.success("DPS statement generated successfully", statement));
////    }
////
////    @PutMapping("/{id}")
////    public ResponseEntity<ApiResponse<DPSResponseDTO>> updateDPS(
////            @PathVariable Long id,
////            @Valid @RequestBody DPSUpdateRequestDTO request,
////            @RequestHeader("Authorization") String token) {
////        log.info("Update DPS request for ID: {}", id);
////
////        String jwt = token.substring(7);
////        String role = jwtUtil.extractRole(jwt);
////        String customerId = jwtUtil.extractCustomerId(jwt);
////
////        DPSResponseDTO existingDPS = dpsService.getDPSById(id);
////
////        // Verify ownership for customers
////        if ("CUSTOMER".equals(role)) {
////            if (!existingDPS.getCustomerId().equals(customerId)) {
////                return ResponseEntity
////                        .status(HttpStatus.FORBIDDEN)
////                        .body(ApiResponse.error("Access denied: You can only update your own DPS accounts"));
////            }
////            // Customers can only update nominee and auto-debit settings
////            request.setStatus(null);
////        }
////
////        DPSResponseDTO response = dpsService.updateDPS(id, request);
////        return ResponseEntity.ok(ApiResponse.success("DPS updated successfully", response));
////    }
////
////    @PatchMapping("/{dpsNumber}/close")
////    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
////    public ResponseEntity<ApiResponse<DPSResponseDTO>> closeDPS(
////            @PathVariable String dpsNumber,
////            @RequestParam(required = false) String reason) {
////        log.info("Close DPS request for: {}", dpsNumber);
////        DPSResponseDTO response = dpsService.closeDPS(dpsNumber, reason);
////        return ResponseEntity.ok(ApiResponse.success("DPS closed successfully", response));
////    }
////
////    @PatchMapping("/{dpsNumber}/mature")
////    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
////    public ResponseEntity<ApiResponse<DPSResponseDTO>> matureDPS(@PathVariable String dpsNumber) {
////        log.info("Mature DPS request for: {}", dpsNumber);
////        DPSResponseDTO response = dpsService.matureDPS(dpsNumber);
////        return ResponseEntity.ok(ApiResponse.success("DPS matured successfully", response));
////    }
////
////    @GetMapping("/calculate-maturity")
////    public ResponseEntity<ApiResponse<DPSMaturityCalculationDTO>> calculateMaturity(
////            @RequestParam BigDecimal monthlyInstallment,
////            @RequestParam Integer tenureMonths,
////            @RequestParam BigDecimal interestRate) {
////        log.info("Calculate DPS maturity for installment: {}, tenure: {}", monthlyInstallment, tenureMonths);
////        DPSMaturityCalculationDTO calculation = dpsService.calculateMaturity(monthlyInstallment, tenureMonths, interestRate);
////        return ResponseEntity.ok(ApiResponse.success("Maturity calculated successfully", calculation));
////    }
////}
////
////
////
////
////
////
////
////
////
////
//////package com.izak.demoBankManagement.controller;
//////
//////import com.izak.demoBankManagement.dto.*;
//////import com.izak.demoBankManagement.exception.DPSMaturityCalculationDTO;
//////import com.izak.demoBankManagement.service.DPSService;
//////import lombok.RequiredArgsConstructor;
//////import lombok.extern.slf4j.Slf4j;
//////import org.springframework.http.HttpStatus;
//////import org.springframework.http.ResponseEntity;
//////import org.springframework.validation.annotation.Validated;
//////import org.springframework.web.bind.annotation.*;
//////
//////import jakarta.validation.Valid;
//////
//////import java.math.BigDecimal;
//////import java.util.List;
//////
//////@RestController
//////@RequestMapping("/api/dps")
//////@RequiredArgsConstructor
//////@Validated
//////@Slf4j
//////@CrossOrigin(origins = "http://localhost:4200")
//////public class DPSController {
//////
//////    private final DPSService dpsService;
//////
//////    @PostMapping
//////    public ResponseEntity<ApiResponse<DPSResponseDTO>> createDPS(
//////            @Valid @RequestBody DPSCreateRequestDTO request) {
//////        log.info("Create DPS request for customer: {}", request.getCustomerId());
//////        DPSResponseDTO response = dpsService.createDPS(request);
//////        return ResponseEntity.status(HttpStatus.CREATED)
//////                .body(ApiResponse.success("DPS account created successfully", response));
//////    }
//////
//////    @GetMapping
//////    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getAllDPS() {
//////        log.info("Get all DPS accounts request");
//////        List<DPSResponseDTO> dpsAccounts = dpsService.getAllDPS();
//////        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
//////    }
//////
//////    @GetMapping("/{id}")
//////    public ResponseEntity<ApiResponse<DPSResponseDTO>> getDPSById(@PathVariable Long id) {
//////        log.info("Get DPS by ID: {}", id);
//////        DPSResponseDTO dps = dpsService.getDPSById(id);
//////        return ResponseEntity.ok(ApiResponse.success("DPS retrieved successfully", dps));
//////    }
//////
//////    @GetMapping("/number/{dpsNumber}")
//////    public ResponseEntity<ApiResponse<DPSResponseDTO>> getDPSByNumber(@PathVariable String dpsNumber) {
//////        log.info("Get DPS by number: {}", dpsNumber);
//////        DPSResponseDTO dps = dpsService.getDPSByNumber(dpsNumber);
//////        return ResponseEntity.ok(ApiResponse.success("DPS retrieved successfully", dps));
//////    }
//////
//////    @GetMapping("/customer/{customerId}")
//////    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByCustomerId(@PathVariable String customerId) {
//////        log.info("Get DPS accounts for customer: {}", customerId);
//////        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByCustomerId(customerId);
//////        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
//////    }
//////
//////    @GetMapping("/branch/{branchId}")
//////    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByBranch(@PathVariable Long branchId) {
//////        log.info("Get DPS accounts for branch: {}", branchId);
//////        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByBranch(branchId);
//////        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
//////    }
//////
//////    @GetMapping("/status/{status}")
//////    public ResponseEntity<ApiResponse<List<DPSResponseDTO>>> getDPSByStatus(@PathVariable String status) {
//////        log.info("Get DPS accounts by status: {}", status);
//////        List<DPSResponseDTO> dpsAccounts = dpsService.getDPSByStatus(status);
//////        return ResponseEntity.ok(ApiResponse.success("DPS accounts retrieved successfully", dpsAccounts));
//////    }
//////
//////    @PostMapping("/pay-installment")
//////    public ResponseEntity<ApiResponse<TransactionResponseDTO>> payInstallment(
//////            @Valid @RequestBody DPSPaymentRequestDTO request) {
//////        log.info("DPS payment request for: {}", request.getDpsNumber());
//////        TransactionResponseDTO response = dpsService.payInstallment(request);
//////        return ResponseEntity.status(HttpStatus.CREATED)
//////                .body(ApiResponse.success("DPS installment paid successfully", response));
//////    }
//////
//////    @GetMapping("/statement/{dpsNumber}")
//////    public ResponseEntity<ApiResponse<DPSStatementDTO>> getDPSStatement(@PathVariable String dpsNumber) {
//////        log.info("DPS statement request for: {}", dpsNumber);
//////        DPSStatementDTO statement = dpsService.getDPSStatement(dpsNumber);
//////        return ResponseEntity.ok(ApiResponse.success("DPS statement generated successfully", statement));
//////    }
//////
//////    @PutMapping("/{id}")
//////    public ResponseEntity<ApiResponse<DPSResponseDTO>> updateDPS(
//////            @PathVariable Long id,
//////            @Valid @RequestBody DPSUpdateRequestDTO request) {
//////        log.info("Update DPS request for ID: {}", id);
//////        DPSResponseDTO response = dpsService.updateDPS(id, request);
//////        return ResponseEntity.ok(ApiResponse.success("DPS updated successfully", response));
//////    }
//////
//////    @PatchMapping("/{dpsNumber}/close")
//////    public ResponseEntity<ApiResponse<DPSResponseDTO>> closeDPS(
//////            @PathVariable String dpsNumber,
//////            @RequestParam(required = false) String reason) {
//////        log.info("Close DPS request for: {}", dpsNumber);
//////        DPSResponseDTO response = dpsService.closeDPS(dpsNumber, reason);
//////        return ResponseEntity.ok(ApiResponse.success("DPS closed successfully", response));
//////    }
//////
//////    @PatchMapping("/{dpsNumber}/mature")
//////    public ResponseEntity<ApiResponse<DPSResponseDTO>> matureDPS(@PathVariable String dpsNumber) {
//////        log.info("Mature DPS request for: {}", dpsNumber);
//////        DPSResponseDTO response = dpsService.matureDPS(dpsNumber);
//////        return ResponseEntity.ok(ApiResponse.success("DPS matured successfully", response));
//////    }
//////
//////    @GetMapping("/calculate-maturity")
//////    public ResponseEntity<ApiResponse<DPSMaturityCalculationDTO>> calculateMaturity(
//////            @RequestParam BigDecimal monthlyInstallment,
//////            @RequestParam Integer tenureMonths,
//////            @RequestParam BigDecimal interestRate) {
//////        log.info("Calculate DPS maturity for installment: {}, tenure: {}", monthlyInstallment, tenureMonths);
//////        DPSMaturityCalculationDTO calculation = dpsService.calculateMaturity(monthlyInstallment, tenureMonths, interestRate);
//////        return ResponseEntity.ok(ApiResponse.success("Maturity calculated successfully", calculation));
//////    }
//////}

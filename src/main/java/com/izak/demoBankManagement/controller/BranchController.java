package com.izak.demoBankManagement.controller;

import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    public ResponseEntity<ApiResponse<BranchResponseDTO>> createBranch(
            @Valid @RequestBody BranchCreateRequestDTO request) {
        log.info("Create branch request: {}", request.getBranchName());
        BranchResponseDTO response = branchService.createBranch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Branch created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchResponseDTO>>> getAllBranches() {
        log.info("Get all branches request");
        List<BranchResponseDTO> branches = branchService.getAllBranches();
        return ResponseEntity.ok(ApiResponse.success("Branches retrieved successfully", branches));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponseDTO>> getBranchById(@PathVariable Long id) {
        log.info("Get branch by ID: {}", id);
        BranchResponseDTO branch = branchService.getBranchById(id);
        return ResponseEntity.ok(ApiResponse.success("Branch retrieved successfully", branch));
    }

    @GetMapping("/code/{branchCode}")
    public ResponseEntity<ApiResponse<BranchResponseDTO>> getBranchByCode(@PathVariable String branchCode) {
        log.info("Get branch by code: {}", branchCode);
        BranchResponseDTO branch = branchService.getBranchByCode(branchCode);
        return ResponseEntity.ok(ApiResponse.success("Branch retrieved successfully", branch));
    }

    @GetMapping("/ifsc/{ifscCode}")
    public ResponseEntity<ApiResponse<BranchResponseDTO>> getBranchByIfsc(@PathVariable String ifscCode) {
        log.info("Get branch by IFSC: {}", ifscCode);
        BranchResponseDTO branch = branchService.getBranchByIfsc(ifscCode);
        return ResponseEntity.ok(ApiResponse.success("Branch retrieved successfully", branch));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse<List<BranchResponseDTO>>> getBranchesByCity(@PathVariable String city) {
        log.info("Get branches by city: {}", city);
        List<BranchResponseDTO> branches = branchService.getBranchesByCity(city);
        return ResponseEntity.ok(ApiResponse.success("Branches retrieved successfully", branches));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<BranchResponseDTO>>> getBranchesByStatus(@PathVariable String status) {
        log.info("Get branches by status: {}", status);
        List<BranchResponseDTO> branches = branchService.getBranchesByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Branches retrieved successfully", branches));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponseDTO>> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody BranchUpdateRequestDTO request) {
        log.info("Update branch request for ID: {}", id);
        BranchResponseDTO response = branchService.updateBranch(id, request);
        return ResponseEntity.ok(ApiResponse.success("Branch updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long id) {
        log.info("Delete branch request for ID: {}", id);
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.success("Branch deleted successfully", null));
    }


    @GetMapping("/{id}/statistics")
    public ResponseEntity<ApiResponse<BranchStatisticsDTO>> getBranchStatistics(@PathVariable Long id) {
        log.info("Get statistics for branch ID: {}", id);
        BranchStatisticsDTO stats = branchService.getBranchStatistics(id);
        return ResponseEntity.ok(ApiResponse.success("Branch statistics retrieved successfully", stats));
    }

    @GetMapping("/statistics/bank")
    public ResponseEntity<ApiResponse<BankStatisticsDTO>> getBankStatistics() {
        log.info("Get overall bank statistics");
        BankStatisticsDTO stats = branchService.getBankStatistics();
        return ResponseEntity.ok(ApiResponse.success("Bank statistics retrieved successfully", stats));
    }
}

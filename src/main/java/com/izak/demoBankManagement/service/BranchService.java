package com.izak.demoBankManagement.service;

import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.entity.Branch;
import com.izak.demoBankManagement.exception.*;
import com.izak.demoBankManagement.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional
    public BranchResponseDTO createBranch(BranchCreateRequestDTO request) {
        log.info("Creating new branch: {}", request.getBranchName());

        if (branchRepository.existsByIfscCode(request.getIfscCode())) {
            throw new DuplicateResourceException("IFSC code already exists: " + request.getIfscCode());
        }

        Branch branch = new Branch();
        branch.setBranchCode(generateBranchCode());
        branch.setBranchName(request.getBranchName());
        branch.setAddress(request.getAddress());
        branch.setCity(request.getCity());
        branch.setState(request.getState());
        branch.setZipCode(request.getZipCode());
        branch.setPhone(request.getPhone());
        branch.setEmail(request.getEmail());
        branch.setManagerName(request.getManagerName());
        branch.setManagerPhone(request.getManagerPhone());
        branch.setManagerEmail(request.getManagerEmail());
        branch.setIfscCode(request.getIfscCode());
        branch.setSwiftCode(request.getSwiftCode());
        branch.setWorkingHours(request.getWorkingHours());
        branch.setIsMainBranch(request.getIsMainBranch() != null ? request.getIsMainBranch() : false);
        branch.setStatus(Branch.BranchStatus.ACTIVE);

        branch = branchRepository.save(branch);
        log.info("Branch created successfully: {}", branch.getBranchCode());

        return mapToResponseDTO(branch);
    }

    public BranchResponseDTO getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + id));
        return mapToResponseDTO(branch);
    }

    public BranchResponseDTO getBranchByCode(String branchCode) {
        Branch branch = branchRepository.findByBranchCode(branchCode)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchCode));
        return mapToResponseDTO(branch);
    }

    public BranchResponseDTO getBranchByIfsc(String ifscCode) {
        Branch branch = branchRepository.findByIfscCode(ifscCode)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with IFSC: " + ifscCode));
        return mapToResponseDTO(branch);
    }

    public List<BranchResponseDTO> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BranchResponseDTO> getBranchesByCity(String city) {
        return branchRepository.findByCity(city).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BranchResponseDTO> getBranchesByStatus(String status) {
        Branch.BranchStatus branchStatus = Branch.BranchStatus.valueOf(status.toUpperCase());
        return branchRepository.findByStatus(branchStatus).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BranchResponseDTO updateBranch(Long id, BranchUpdateRequestDTO request) {
        log.info("Updating branch with ID: {}", id);

        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + id));

        if (request.getBranchName() != null) branch.setBranchName(request.getBranchName());
        if (request.getAddress() != null) branch.setAddress(request.getAddress());
        if (request.getCity() != null) branch.setCity(request.getCity());
        if (request.getState() != null) branch.setState(request.getState());
        if (request.getZipCode() != null) branch.setZipCode(request.getZipCode());
        if (request.getPhone() != null) branch.setPhone(request.getPhone());
        if (request.getEmail() != null) branch.setEmail(request.getEmail());
        if (request.getManagerName() != null) branch.setManagerName(request.getManagerName());
        if (request.getManagerPhone() != null) branch.setManagerPhone(request.getManagerPhone());
        if (request.getManagerEmail() != null) branch.setManagerEmail(request.getManagerEmail());
        if (request.getWorkingHours() != null) branch.setWorkingHours(request.getWorkingHours());
        if (request.getStatus() != null) {
            branch.setStatus(Branch.BranchStatus.valueOf(request.getStatus().toUpperCase()));
        }

        branch = branchRepository.save(branch);
        log.info("Branch updated successfully: {}", branch.getBranchCode());

        return mapToResponseDTO(branch);
    }

    @Transactional
    public void deleteBranch(Long id) {
        log.info("Deleting branch with ID: {}", id);

        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + id));

        branch.setStatus(Branch.BranchStatus.INACTIVE);
        branchRepository.save(branch);

        log.info("Branch soft deleted: {}", branch.getBranchCode());
    }

    private String generateBranchCode() {
        String branchCode;
        do {
            int randomNum = new Random().nextInt(9000) + 1000;
            branchCode = "BR" + randomNum;
        } while (branchRepository.existsByBranchCode(branchCode));
        return branchCode;
    }

//    private BranchResponseDTO mapToResponseDTO(Branch branch) {
//        BranchResponseDTO dto = new BranchResponseDTO();
//        dto.setId(branch.getId());
//        dto.setBranchCode(branch.getBranchCode());
//        dto.setBranchName(branch.getBranchName());
//        dto.setAddress(branch.getAddress());
//        dto.setCity(branch.getCity());
//        dto.setState(branch.getState());
//        dto.setZipCode(branch.getZipCode());
//        dto.setPhone(branch.getPhone());
//        dto.setEmail(branch.getEmail());
//        dto.setManagerName(branch.getManagerName());
//        dto.setManagerPhone(branch.getManagerPhone());
//        dto.setManagerEmail(branch.getManagerEmail());
//        dto.setIfscCode(branch.getIfscCode());
//        dto.setSwiftCode(branch.getSwiftCode());
//        dto.setStatus(branch.getStatus().name().toLowerCase());
//        dto.setWorkingHours(branch.getWorkingHours());
//        dto.setIsMainBranch(branch.getIsMainBranch());
//        dto.setCreatedDate(branch.getCreatedDate());
//        dto.setLastUpdated(branch.getLastUpdated());
//        dto.setTotalAccounts(branch.getAccounts() != null ? branch.getAccounts().size() : 0);
//        dto.setTotalDPS(branch.getDpsAccounts() != null ? branch.getDpsAccounts().size() : 0);
//        dto.setTotalEmployees(branch.getEmployees() != null ? branch.getEmployees().size() : 0);
//        return dto;
//    }

    private BranchResponseDTO mapToResponseDTO(Branch branch) {
        BranchResponseDTO dto = new BranchResponseDTO();
        dto.setId(branch.getId());
        dto.setBranchCode(branch.getBranchCode());
        dto.setBranchName(branch.getBranchName());
        dto.setAddress(branch.getAddress());
        dto.setCity(branch.getCity());
        dto.setState(branch.getState());
        dto.setZipCode(branch.getZipCode());
        dto.setPhone(branch.getPhone());
        dto.setEmail(branch.getEmail());
        dto.setManagerName(branch.getManagerName());
        dto.setManagerPhone(branch.getManagerPhone());
        dto.setManagerEmail(branch.getManagerEmail());
        dto.setIfscCode(branch.getIfscCode());
        dto.setSwiftCode(branch.getSwiftCode());
        dto.setStatus(branch.getStatus().name().toLowerCase());
        dto.setWorkingHours(branch.getWorkingHours());
        dto.setIsMainBranch(branch.getIsMainBranch());
        dto.setCreatedDate(branch.getCreatedDate());
        dto.setLastUpdated(branch.getLastUpdated());

        // Get actual counts from database
        Long activeAccountCount = branchRepository.getActiveAccountCountByBranch(branch.getId());
        dto.setTotalAccounts(activeAccountCount != null ? activeAccountCount.intValue() : 0);
        dto.setTotalDPS(branch.getDpsAccounts() != null ? branch.getDpsAccounts().size() : 0);
        dto.setTotalEmployees(branch.getEmployees() != null ? branch.getEmployees().size() : 0);

        return dto;
    }

    public BranchStatisticsDTO getBranchStatistics(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + branchId));

        Long activeAccountCount = branchRepository.getActiveAccountCountByBranch(branchId);
        BigDecimal totalBalance = branchRepository.getTotalBalanceByBranch(branchId);

        if (totalBalance == null) totalBalance = BigDecimal.ZERO;

        BigDecimal averageBalance = activeAccountCount > 0
                ? totalBalance.divide(new BigDecimal(activeAccountCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BranchStatisticsDTO stats = new BranchStatisticsDTO();
        stats.setBranchId(branch.getId());
        stats.setBranchCode(branch.getBranchCode());
        stats.setBranchName(branch.getBranchName());
        stats.setCity(branch.getCity());
        stats.setTotalAccounts(branch.getAccounts() != null ? branch.getAccounts().size() : 0);
        stats.setActiveAccounts(activeAccountCount.intValue());
        stats.setTotalBalance(totalBalance);
        stats.setAverageBalance(averageBalance);

        return stats;
    }

    public BankStatisticsDTO getBankStatistics() {
        List<Branch> allBranches = branchRepository.findAll();
        List<Branch> activeBranches = branchRepository.findByStatus(Branch.BranchStatus.ACTIVE);

        Long totalAccounts = branchRepository.getTotalActiveAccounts();
        BigDecimal totalBalance = branchRepository.getTotalBankBalance();

        if (totalBalance == null) totalBalance = BigDecimal.ZERO;
        if (totalAccounts == null) totalAccounts = 0L;

        BigDecimal averageBalance = totalAccounts > 0
                ? totalBalance.divide(new BigDecimal(totalAccounts), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get statistics for each branch
        List<BranchStatisticsDTO> branchStats = allBranches.stream()
                .map(branch -> getBranchStatistics(branch.getId()))
                .collect(Collectors.toList());

        BankStatisticsDTO bankStats = new BankStatisticsDTO();
        bankStats.setTotalBranches(allBranches.size());
        bankStats.setActiveBranches(activeBranches.size());
        bankStats.setTotalAccounts(totalAccounts);
        bankStats.setActiveAccounts(totalAccounts);
        bankStats.setTotalBankBalance(totalBalance);
        bankStats.setAverageBalancePerAccount(averageBalance);
        bankStats.setBranchStatistics(branchStats);

        return bankStats;
    }

}

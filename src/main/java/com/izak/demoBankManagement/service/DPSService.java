package com.izak.demoBankManagement.service;

import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.entity.*;
import com.izak.demoBankManagement.exception.*;
import com.izak.demoBankManagement.repository.*;
import com.izak.demoBankManagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DPSService {

    private final DPSRepository dpsRepository;
    private final DPSInstallmentRepository installmentRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final BranchRepository branchRepository;
    private final TransactionRepository transactionRepository;
    private final JwtUtil jwtUtil;
    private final BranchAuthorizationService branchAuthService;

    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.02"); // 2% penalty per missed installment

    /**
     * Create a new DPS account with branch-level authorization
     * ADMIN: Can create in any branch
     * BRANCH_MANAGER: Can only create in their assigned branch
     * Others: Access denied
     */
    @Transactional
    public DPSResponseDTO createDPS(DPSCreateRequestDTO request, String jwtToken) {
        log.info("Creating new DPS for customer: {}", request.getCustomerId());

        // Extract authentication details
        String role = jwtUtil.extractRole(jwtToken);
        Long userBranchId = jwtUtil.extractBranchId(jwtToken);

        // Validate customer exists
        Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + request.getCustomerId()));

        // Validate branch exists
        Branch branch = branchRepository.findByBranchCode(request.getBranchCode())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchCode()));

        // AUTHORIZATION CHECK: Branch Managers can only create DPS in their assigned branch
        if ("BRANCH_MANAGER".equals(role)) {
            if (userBranchId == null || !branch.getId().equals(userBranchId)) {
                throw new UnauthorizedAccessException(
                        "Access denied: You can only create DPS accounts in your assigned branch");
            }
        } else if ("LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
            throw new UnauthorizedAccessException(
                    "Access denied: Your role does not have permission for DPS operations");
        } else if ("CUSTOMER".equals(role)) {
            throw new UnauthorizedAccessException(
                    "Access denied: Customers cannot directly create DPS accounts");
        }
        // ADMIN role proceeds without additional checks

        // Validate linked account if provided
        Account linkedAccount = null;
        if (request.getLinkedAccountNumber() != null) {
            linkedAccount = accountRepository.findByAccountNumber(request.getLinkedAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Linked account not found: " + request.getLinkedAccountNumber()));
        }

        // Calculate maturity amount and maturity date
        BigDecimal maturityAmount = calculateMaturityAmount(
                request.getMonthlyInstallment(),
                request.getTenureMonths(),
                request.getInterestRate()
        );

        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(request.getTenureMonths());

        // Create DPS entity
        DPS dps = new DPS();
        dps.setDpsNumber(generateDPSNumber());
        dps.setCustomer(customer);
        dps.setCustomerId(customer.getCustomerId());
        dps.setLinkedAccount(linkedAccount);
        dps.setBranch(branch);
        dps.setMonthlyInstallment(request.getMonthlyInstallment());
        dps.setTenureMonths(request.getTenureMonths());
        dps.setInterestRate(request.getInterestRate());
        dps.setMaturityAmount(maturityAmount);
        dps.setStartDate(startDate);
        dps.setMaturityDate(maturityDate);
        dps.setNextPaymentDate(startDate.plusMonths(1));
        dps.setPendingInstallments(request.getTenureMonths());
        dps.setAutoDebitEnabled(request.getAutoDebitEnabled() != null ? request.getAutoDebitEnabled() : false);
        dps.setNomineeFirstName(request.getNomineeFirstName());
        dps.setNomineeLastName(request.getNomineeLastName());
        dps.setNomineeRelationship(request.getNomineeRelationship());
        dps.setNomineePhone(request.getNomineePhone());
        dps.setRemarks(request.getRemarks());
        dps.setStatus(DPS.DPSStatus.ACTIVE);

        dps = dpsRepository.save(dps);

        // Generate installment schedule
        generateInstallmentSchedule(dps);

        log.info("DPS created successfully: {}", dps.getDpsNumber());
        return mapToResponseDTO(dps);
    }

    /**
     * Process DPS installment payment with branch-level authorization
     */
    @Transactional
    public TransactionResponseDTO payInstallment(DPSPaymentRequestDTO request, String jwtToken) {
        log.info("Processing DPS payment for: {}", request.getDpsNumber());

        // Fetch DPS account
        DPS dps = dpsRepository.findByDpsNumber(request.getDpsNumber())
                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + request.getDpsNumber()));

        // AUTHORIZATION CHECK: Use BranchAuthorizationService
        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
            throw new UnauthorizedAccessException(
                    "Access denied: You do not have permission to process payments for this DPS account");
        }

        // Validate DPS status
        if (dps.getStatus() != DPS.DPSStatus.ACTIVE) {
            throw new InvalidTransactionException("DPS is not active");
        }

        // Find next pending installment
        List<DPSInstallment> pendingInstallments = installmentRepository
                .findByDpsIdAndStatus(dps.getId(), DPSInstallment.InstallmentStatus.PENDING);

        if (pendingInstallments.isEmpty()) {
            throw new InvalidTransactionException("No pending installments");
        }

        DPSInstallment installment = pendingInstallments.get(0);

        // Calculate penalty if overdue
        BigDecimal penalty = BigDecimal.ZERO;
        if (LocalDate.now().isAfter(installment.getDueDate())) {
            penalty = dps.getMonthlyInstallment().multiply(PENALTY_RATE);
            installment.setPenaltyAmount(penalty);
        }

        BigDecimal totalAmount = request.getAmount().add(penalty);

        // Update installment
        installment.setStatus(DPSInstallment.InstallmentStatus.PAID);
        installment.setPaymentDate(LocalDate.now());
        installment.setTotalPaid(totalAmount);
        installment.setPaymentMode(request.getPaymentMode());
        installment.setRemarks(request.getRemarks());
        installment.setTransactionId(generateTransactionId());
        installment.setReceiptNumber(generateReceiptNumber());
        installmentRepository.save(installment);

        // Update DPS
        dps.setTotalDeposited(dps.getTotalDeposited().add(request.getAmount()));
        dps.setTotalInstallmentsPaid(dps.getTotalInstallmentsPaid() + 1);
        dps.setPendingInstallments(dps.getPendingInstallments() - 1);
        dps.setLastPaymentDate(LocalDate.now());
        dps.setPenaltyAmount(dps.getPenaltyAmount().add(penalty));

        // Find next pending installment for next payment date
        List<DPSInstallment> remainingPending = installmentRepository
                .findByDpsIdAndStatus(dps.getId(), DPSInstallment.InstallmentStatus.PENDING);
        if (!remainingPending.isEmpty()) {
            dps.setNextPaymentDate(remainingPending.get(0).getDueDate());
        } else {
            dps.setNextPaymentDate(null);
            dps.setStatus(DPS.DPSStatus.MATURED);
            dps.setMaturedDate(LocalDateTime.now());
        }

        dpsRepository.save(dps);

        log.info("DPS installment paid successfully");

        // Create transaction response
        TransactionResponseDTO response = new TransactionResponseDTO();
        response.setTransactionId(installment.getTransactionId());
        response.setReferenceNumber(installment.getReceiptNumber());
        response.setAmount(request.getAmount());
        response.setTransferFee(penalty);
        response.setTotalAmount(totalAmount);
        response.setStatus("COMPLETED");
        response.setDescription("DPS Installment Payment - " + installment.getInstallmentNumber());
        response.setTimestamp(LocalDateTime.now().toString());

        return response;
    }

    /**
     * Get DPS statement with branch-level authorization
     */
    public DPSStatementDTO getDPSStatement(String dpsNumber, String jwtToken) {
        log.info("Fetching DPS statement for: {}", dpsNumber);

        // Fetch DPS account
        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));

        // AUTHORIZATION CHECK: Use BranchAuthorizationService
        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
            throw new UnauthorizedAccessException(
                    "Access denied: You do not have permission to view this DPS statement");
        }

        List<DPSInstallment> installments = installmentRepository.findByDpsId(dps.getId());

        DPSStatementDTO statement = new DPSStatementDTO();
        statement.setDpsNumber(dps.getDpsNumber());
        statement.setCustomerName(dps.getCustomer().getFirstName() + " " + dps.getCustomer().getLastName());
        statement.setMonthlyInstallment(dps.getMonthlyInstallment());
        statement.setTotalInstallments(dps.getTenureMonths());
        statement.setPaidInstallments(dps.getTotalInstallmentsPaid());
        statement.setPendingInstallments(dps.getPendingInstallments());
        statement.setTotalDeposited(dps.getTotalDeposited());
        statement.setMaturityAmount(dps.getMaturityAmount());
        statement.setMaturityDate(dps.getMaturityDate());

        List<DPSInstallmentDTO> installmentDTOs = installments.stream()
                .map(this::mapToInstallmentDTO)
                .collect(Collectors.toList());
        statement.setInstallments(installmentDTOs);

        return statement;
    }

    /**
     * Close DPS account with branch-level authorization
     */
    @Transactional
    public DPSResponseDTO closeDPS(String dpsNumber, String reason, String jwtToken) {
        log.info("Closing DPS: {}", dpsNumber);

        // Fetch DPS account
        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));

        // AUTHORIZATION CHECK: Use BranchAuthorizationService
        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
            throw new UnauthorizedAccessException(
                    "Access denied: You do not have permission to close this DPS account");
        }

        if (dps.getStatus() == DPS.DPSStatus.CLOSED) {
            throw new InvalidTransactionException("DPS is already closed");
        }

        dps.setStatus(DPS.DPSStatus.CLOSED);
        dps.setClosedDate(LocalDateTime.now());
        dps.setRemarks(reason != null ? reason : dps.getRemarks());

        dpsRepository.save(dps);

        log.info("DPS closed successfully");
        return mapToResponseDTO(dps);
    }

    /**
     * Mature DPS account with branch-level authorization
     */
    @Transactional
    public DPSResponseDTO matureDPS(String dpsNumber, String jwtToken) {
        log.info("Maturing DPS: {}", dpsNumber);

        // Fetch DPS account
        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));

        // AUTHORIZATION CHECK: Use BranchAuthorizationService
        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
            throw new UnauthorizedAccessException(
                    "Access denied: You do not have permission to mature this DPS account");
        }

        if (dps.getPendingInstallments() > 0) {
            throw new InvalidTransactionException("Cannot mature DPS with pending installments");
        }

        dps.setStatus(DPS.DPSStatus.MATURED);
        dps.setMaturedDate(LocalDateTime.now());

        dpsRepository.save(dps);

        log.info("DPS matured successfully");
        return mapToResponseDTO(dps);
    }

    /**
     * Calculate maturity for DPS (no authorization required - calculation only)
     */
    public DPSMaturityCalculationDTO calculateMaturity(
            BigDecimal monthlyInstallment,
            Integer tenureMonths,
            BigDecimal interestRate) {

        BigDecimal maturityAmount = calculateMaturityAmount(monthlyInstallment, tenureMonths, interestRate);
        BigDecimal totalDeposit = monthlyInstallment.multiply(new BigDecimal(tenureMonths));
        BigDecimal interestEarned = maturityAmount.subtract(totalDeposit);

        DPSMaturityCalculationDTO dto = new DPSMaturityCalculationDTO();
        dto.setMonthlyInstallment(monthlyInstallment);
        dto.setTenureMonths(tenureMonths);
        dto.setInterestRate(interestRate);
        dto.setTotalDeposit(totalDeposit);
        dto.setInterestEarned(interestEarned);
        dto.setMaturityAmount(maturityAmount);

        return dto;
    }

    /**
     * Get DPS by ID with branch-level authorization
     */
    public DPSResponseDTO getDPSById(Long id, String jwtToken) {
        log.info("Fetching DPS by ID: {}", id);

        // Fetch DPS account
        DPS dps = dpsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DPS not found with ID: " + id));

        // AUTHORIZATION CHECK: Use BranchAuthorizationService
        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
            throw new UnauthorizedAccessException(
                    "Access denied: You do not have permission to view this DPS account");
        }

        return mapToResponseDTO(dps);
    }

    /**
     * Get DPS by number with branch-level authorization
     */
    public DPSResponseDTO getDPSByNumber(String dpsNumber, String jwtToken) {
        log.info("Fetching DPS by number: {}", dpsNumber);

        // Fetch DPS account
        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));

        // AUTHORIZATION CHECK: Use BranchAuthorizationService
        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
            throw new UnauthorizedAccessException(
                    "Access denied: You do not have permission to view this DPS account");
        }

        return mapToResponseDTO(dps);
    }

    /**
     * Get all DPS accounts with branch-level authorization
     * ADMIN: Can view all DPS accounts
     * BRANCH_MANAGER: Can only view DPS accounts in their assigned branch
     * Others: Access denied
     */
    public List<DPSResponseDTO> getAllDPS(String jwtToken) {
        log.info("Fetching all DPS accounts");

        String role = jwtUtil.extractRole(jwtToken);
        Long userBranchId = jwtUtil.extractBranchId(jwtToken);

        List<DPS> dpsList;

        if ("ADMIN".equals(role)) {
            // Admin sees all DPS accounts
            dpsList = dpsRepository.findAll();
        } else if ("BRANCH_MANAGER".equals(role)) {
            // Branch manager sees only their branch
            if (userBranchId == null) {
                throw new UnauthorizedAccessException(
                        "Branch manager must be assigned to a branch");
            }
            dpsList = dpsRepository.findByBranchId(userBranchId);
        } else {
            throw new UnauthorizedAccessException(
                    "Access denied: Insufficient permissions to list all DPS accounts");
        }

        return dpsList.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get DPS by customer ID with authorization
     * ADMIN: Can view any customer's DPS
     * CUSTOMER: Can only view their own DPS
     * BRANCH_MANAGER: Can view DPS for customers in their branch
     */
    public List<DPSResponseDTO> getDPSByCustomerId(String customerId, String jwtToken) {
        log.info("Fetching DPS for customer: {}", customerId);

        String role = jwtUtil.extractRole(jwtToken);
        String tokenCustomerId = jwtUtil.extractCustomerId(jwtToken);
        Long userBranchId = jwtUtil.extractBranchId(jwtToken);

        // Authorization check based on role
        if ("CUSTOMER".equals(role)) {
            if (!customerId.equals(tokenCustomerId)) {
                throw new UnauthorizedAccessException(
                        "Access denied: You can only view your own DPS accounts");
            }
        } else if ("BRANCH_MANAGER".equals(role)) {
            // Branch Manager can only view DPS in their branch
            if (userBranchId == null) {
                throw new UnauthorizedAccessException(
                        "Branch manager must be assigned to a branch");
            }
        } else if (!"ADMIN".equals(role)) {
            throw new UnauthorizedAccessException(
                    "Access denied: Your role does not have permission for DPS operations");
        }

        List<DPS> dpsList = dpsRepository.findByCustomerId(customerId);

        // Filter by branch for Branch Managers
        if ("BRANCH_MANAGER".equals(role)) {
            dpsList = dpsList.stream()
                    .filter(dps -> dps.getBranch() != null && dps.getBranch().getId().equals(userBranchId))
                    .collect(Collectors.toList());
        }

        return dpsList.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get DPS by branch with authorization
     * ADMIN: Can view DPS for any branch
     * BRANCH_MANAGER: Can only view DPS for their assigned branch
     * Others: Access denied
     */
    public List<DPSResponseDTO> getDPSByBranch(Long branchId, String jwtToken) {
        log.info("Fetching DPS for branch: {}", branchId);

        String role = jwtUtil.extractRole(jwtToken);
        Long userBranchId = jwtUtil.extractBranchId(jwtToken);

        // AUTHORIZATION CHECK: Branch Managers can only view their own branch
        if ("BRANCH_MANAGER".equals(role)) {
            if (userBranchId == null) {
                throw new UnauthorizedAccessException(
                        "Branch manager must be assigned to a branch");
            }
            if (!branchId.equals(userBranchId)) {
                throw new UnauthorizedAccessException(
                        "Access denied: You can only view DPS accounts in your assigned branch");
            }
        } else if ("CUSTOMER".equals(role) || "LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
            throw new UnauthorizedAccessException(
                    "Access denied: Your role does not have permission to view branch DPS accounts");
        }
        // ADMIN role proceeds without additional checks

        return dpsRepository.findByBranchId(branchId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get DPS by status with authorization
     * ADMIN: Can view DPS with any status across all branches
     * BRANCH_MANAGER: Can only view DPS with specified status in their branch
     * Others: Access denied
     */
    public List<DPSResponseDTO> getDPSByStatus(String status, String jwtToken) {
        log.info("Fetching DPS with status: {}", status);

        String role = jwtUtil.extractRole(jwtToken);
        Long userBranchId = jwtUtil.extractBranchId(jwtToken);

        DPS.DPSStatus dpsStatus = DPS.DPSStatus.valueOf(status.toUpperCase());
        List<DPS> dpsList;

        if ("ADMIN".equals(role)) {
            // Admin can view all DPS with specified status
            dpsList = dpsRepository.findByStatus(dpsStatus);
        } else if ("BRANCH_MANAGER".equals(role)) {
            // Branch manager can only view DPS in their branch with specified status
            if (userBranchId == null) {
                throw new UnauthorizedAccessException(
                        "Branch manager must be assigned to a branch");
            }
            dpsList = dpsRepository.findByBranchIdAndStatus(userBranchId, dpsStatus);
        } else {
            throw new UnauthorizedAccessException(
                    "Access denied: Your role does not have permission to query DPS by status");
        }

        return dpsList.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update DPS with branch-level authorization
     */
    @Transactional
    public DPSResponseDTO updateDPS(Long id, DPSUpdateRequestDTO request, String jwtToken) {
        log.info("Updating DPS with ID: {}", id);

        // Fetch DPS account
        DPS dps = dpsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DPS not found with ID: " + id));

        // AUTHORIZATION CHECK: Use BranchAuthorizationService
        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
            throw new UnauthorizedAccessException(
                    "Access denied: You do not have permission to update this DPS account");
        }

        // Update fields if provided
        if (request.getLinkedAccountNumber() != null) {
            Account account = accountRepository.findByAccountNumber(request.getLinkedAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));
            dps.setLinkedAccount(account);
        }
        if (request.getAutoDebitEnabled() != null) dps.setAutoDebitEnabled(request.getAutoDebitEnabled());
        if (request.getNomineeFirstName() != null) dps.setNomineeFirstName(request.getNomineeFirstName());
        if (request.getNomineeLastName() != null) dps.setNomineeLastName(request.getNomineeLastName());
        if (request.getNomineeRelationship() != null) dps.setNomineeRelationship(request.getNomineeRelationship());
        if (request.getNomineePhone() != null) dps.setNomineePhone(request.getNomineePhone());
        if (request.getRemarks() != null) dps.setRemarks(request.getRemarks());
        if (request.getStatus() != null) {
            dps.setStatus(DPS.DPSStatus.valueOf(request.getStatus().toUpperCase()));
        }

        dpsRepository.save(dps);
        log.info("DPS updated successfully");
        return mapToResponseDTO(dps);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Generate installment schedule for DPS
     */
    private void generateInstallmentSchedule(DPS dps) {
        List<DPSInstallment> installments = new ArrayList<>();
        LocalDate dueDate = dps.getStartDate().plusMonths(1);

        for (int i = 1; i <= dps.getTenureMonths(); i++) {
            DPSInstallment installment = new DPSInstallment();
            installment.setDps(dps);
            installment.setInstallmentNumber(i);
            installment.setDueDate(dueDate);
            installment.setAmount(dps.getMonthlyInstallment());
            installment.setStatus(DPSInstallment.InstallmentStatus.PENDING);
            installments.add(installment);
            dueDate = dueDate.plusMonths(1);
        }

        installmentRepository.saveAll(installments);
    }

    /**
     * Calculate maturity amount using compound interest formula
     */
    private BigDecimal calculateMaturityAmount(BigDecimal monthlyInstallment, Integer tenureMonths, BigDecimal interestRate) {
        // Using compound interest formula for monthly deposits
        BigDecimal monthlyRate = interestRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusRate.pow(tenureMonths);
        BigDecimal numerator = power.subtract(BigDecimal.ONE);
        BigDecimal maturity = monthlyInstallment.multiply(numerator).divide(monthlyRate, 2, RoundingMode.HALF_UP);
        return maturity.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generate unique DPS number
     */
    private String generateDPSNumber() {
        String dpsNumber;
        do {
            long randomNum = (long) (Math.random() * 10000000000L);
            dpsNumber = "DPS" + randomNum;
        } while (dpsRepository.existsByDpsNumber(dpsNumber));
        return dpsNumber;
    }

    /**
     * Generate unique transaction ID
     */
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis();
    }

    /**
     * Generate unique receipt number
     */
    private String generateReceiptNumber() {
        return "RCP" + System.currentTimeMillis();
    }

    /**
     * Map DPS entity to response DTO
     */
    private DPSResponseDTO mapToResponseDTO(DPS dps) {
        DPSResponseDTO dto = new DPSResponseDTO();
        dto.setId(dps.getId());
        dto.setDpsNumber(dps.getDpsNumber());
        dto.setCustomerId(dps.getCustomerId());
        dto.setCustomerName(dps.getCustomer().getFirstName() + " " + dps.getCustomer().getLastName());
        dto.setLinkedAccountNumber(dps.getLinkedAccount() != null ? dps.getLinkedAccount().getAccountNumber() : null);
        dto.setBranchName(dps.getBranch().getBranchName());
        dto.setMonthlyInstallment(dps.getMonthlyInstallment());
        dto.setTenureMonths(dps.getTenureMonths());
        dto.setInterestRate(dps.getInterestRate());
        dto.setMaturityAmount(dps.getMaturityAmount());
        dto.setTotalDeposited(dps.getTotalDeposited());
        dto.setTotalInstallmentsPaid(dps.getTotalInstallmentsPaid());
        dto.setPendingInstallments(dps.getPendingInstallments());
        dto.setStartDate(dps.getStartDate());
        dto.setMaturityDate(dps.getMaturityDate());
        dto.setNextPaymentDate(dps.getNextPaymentDate());
        dto.setStatus(dps.getStatus().name().toLowerCase());
        dto.setAutoDebitEnabled(dps.getAutoDebitEnabled());
        dto.setPenaltyAmount(dps.getPenaltyAmount());
        dto.setMissedInstallments(dps.getMissedInstallments());
        dto.setCurrency(dps.getCurrency());
        dto.setNomineeFirstName(dps.getNomineeFirstName());
        dto.setNomineeLastName(dps.getNomineeLastName());
        dto.setCreatedDate(dps.getCreatedDate());
        return dto;
    }

    /**
     * Map installment entity to DTO
     */
    private DPSInstallmentDTO mapToInstallmentDTO(DPSInstallment installment) {
        DPSInstallmentDTO dto = new DPSInstallmentDTO();
        dto.setInstallmentNumber(installment.getInstallmentNumber());
        dto.setDueDate(installment.getDueDate());
        dto.setPaymentDate(installment.getPaymentDate());
        dto.setAmount(installment.getAmount());
        dto.setPenaltyAmount(installment.getPenaltyAmount());
        dto.setStatus(installment.getStatus().name().toLowerCase());
        dto.setTransactionId(installment.getTransactionId());
        dto.setReceiptNumber(installment.getReceiptNumber());
        return dto;
    }
}










//package com.izak.demoBankManagement.service;
//
//import com.izak.demoBankManagement.dto.*;
//import com.izak.demoBankManagement.entity.*;
//import com.izak.demoBankManagement.exception.*;
//import com.izak.demoBankManagement.repository.*;
//import com.izak.demoBankManagement.security.JwtUtil;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class DPSService {
//
//    private final DPSRepository dpsRepository;
//    private final DPSInstallmentRepository installmentRepository;
//    private final CustomerRepository customerRepository;
//    private final AccountRepository accountRepository;
//    private final BranchRepository branchRepository;
//    private final TransactionRepository transactionRepository;
//    private final JwtUtil jwtUtil;
//    private final BranchAuthorizationService branchAuthService;
//
//    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.02"); // 2% penalty per missed installment
//
//    /**
//     * Create a new DPS account with branch-level authorization
//     * ADMIN: Can create in any branch
//     * BRANCH_MANAGER: Can only create in their assigned branch
//     * Others: Access denied
//     */
//    @Transactional
//    public DPSResponseDTO createDPS(DPSCreateRequestDTO request, String jwtToken) {
//        log.info("Creating new DPS for customer: {}", request.getCustomerId());
//
//        // Extract authentication details
//        String role = jwtUtil.extractRole(jwtToken);
//        Long userBranchId = jwtUtil.extractBranchId(jwtToken);
//
//        // Validate customer exists
//        Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
//                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + request.getCustomerId()));
//
//        // Validate branch exists
//        Branch branch = branchRepository.findByBranchCode(request.getBranchCode())
//                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchCode()));
//
//        // AUTHORIZATION CHECK: Branch Managers can only create DPS in their assigned branch
//        if ("BRANCH_MANAGER".equals(role)) {
//            if (userBranchId == null || !branch.getId().equals(userBranchId)) {
//                throw new UnauthorizedAccessException(
//                        "Access denied: You can only create DPS accounts in your assigned branch");
//            }
//        } else if ("LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: Your role does not have permission for DPS operations");
//        } else if ("CUSTOMER".equals(role)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: Customers cannot directly create DPS accounts");
//        }
//        // ADMIN role proceeds without additional checks
//
//        // Validate linked account if provided
//        Account linkedAccount = null;
//        if (request.getLinkedAccountNumber() != null) {
//            linkedAccount = accountRepository.findByAccountNumber(request.getLinkedAccountNumber())
//                    .orElseThrow(() -> new AccountNotFoundException("Linked account not found: " + request.getLinkedAccountNumber()));
//        }
//
//        // Calculate maturity amount and maturity date
//        BigDecimal maturityAmount = calculateMaturityAmount(
//                request.getMonthlyInstallment(),
//                request.getTenureMonths(),
//                request.getInterestRate()
//        );
//
//        LocalDate startDate = LocalDate.now();
//        LocalDate maturityDate = startDate.plusMonths(request.getTenureMonths());
//
//        // Create DPS entity
//        DPS dps = new DPS();
//        dps.setDpsNumber(generateDPSNumber());
//        dps.setCustomer(customer);
//        dps.setCustomerId(customer.getCustomerId());
//        dps.setLinkedAccount(linkedAccount);
//        dps.setBranch(branch);
//        dps.setMonthlyInstallment(request.getMonthlyInstallment());
//        dps.setTenureMonths(request.getTenureMonths());
//        dps.setInterestRate(request.getInterestRate());
//        dps.setMaturityAmount(maturityAmount);
//        dps.setStartDate(startDate);
//        dps.setMaturityDate(maturityDate);
//        dps.setNextPaymentDate(startDate.plusMonths(1));
//        dps.setPendingInstallments(request.getTenureMonths());
//        dps.setAutoDebitEnabled(request.getAutoDebitEnabled() != null ? request.getAutoDebitEnabled() : false);
//        dps.setNomineeFirstName(request.getNomineeFirstName());
//        dps.setNomineeLastName(request.getNomineeLastName());
//        dps.setNomineeRelationship(request.getNomineeRelationship());
//        dps.setNomineePhone(request.getNomineePhone());
//        dps.setRemarks(request.getRemarks());
//        dps.setStatus(DPS.DPSStatus.ACTIVE);
//
//        dps = dpsRepository.save(dps);
//
//        // Generate installment schedule
//        generateInstallmentSchedule(dps);
//
//        log.info("DPS created successfully: {}", dps.getDpsNumber());
//        return mapToResponseDTO(dps);
//    }
//
//    /**
//     * Process DPS installment payment with branch-level authorization
//     */
//    @Transactional
//    public TransactionResponseDTO payInstallment(DPSPaymentRequestDTO request, String jwtToken) {
//        log.info("Processing DPS payment for: {}", request.getDpsNumber());
//
//        // Fetch DPS account
//        DPS dps = dpsRepository.findByDpsNumber(request.getDpsNumber())
//                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + request.getDpsNumber()));
//
//        // AUTHORIZATION CHECK: Use BranchAuthorizationService
//        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: You do not have permission to process payments for this DPS account");
//        }
//
//        // Validate DPS status
//        if (dps.getStatus() != DPS.DPSStatus.ACTIVE) {
//            throw new InvalidTransactionException("DPS is not active");
//        }
//
//        // Find next pending installment
//        List<DPSInstallment> pendingInstallments = installmentRepository
//                .findByDpsIdAndStatus(dps.getId(), DPSInstallment.InstallmentStatus.PENDING);
//
//        if (pendingInstallments.isEmpty()) {
//            throw new InvalidTransactionException("No pending installments");
//        }
//
//        DPSInstallment installment = pendingInstallments.get(0);
//
//        // Calculate penalty if overdue
//        BigDecimal penalty = BigDecimal.ZERO;
//        if (LocalDate.now().isAfter(installment.getDueDate())) {
//            penalty = dps.getMonthlyInstallment().multiply(PENALTY_RATE);
//            installment.setPenaltyAmount(penalty);
//        }
//
//        BigDecimal totalAmount = request.getAmount().add(penalty);
//
//        // Update installment
//        installment.setStatus(DPSInstallment.InstallmentStatus.PAID);
//        installment.setPaymentDate(LocalDate.now());
//        installment.setTotalPaid(totalAmount);
//        installment.setPaymentMode(request.getPaymentMode());
//        installment.setRemarks(request.getRemarks());
//        installment.setTransactionId(generateTransactionId());
//        installment.setReceiptNumber(generateReceiptNumber());
//        installmentRepository.save(installment);
//
//        // Update DPS
//        dps.setTotalDeposited(dps.getTotalDeposited().add(request.getAmount()));
//        dps.setTotalInstallmentsPaid(dps.getTotalInstallmentsPaid() + 1);
//        dps.setPendingInstallments(dps.getPendingInstallments() - 1);
//        dps.setLastPaymentDate(LocalDate.now());
//        dps.setPenaltyAmount(dps.getPenaltyAmount().add(penalty));
//
//        // Find next pending installment for next payment date
//        List<DPSInstallment> remainingPending = installmentRepository
//                .findByDpsIdAndStatus(dps.getId(), DPSInstallment.InstallmentStatus.PENDING);
//        if (!remainingPending.isEmpty()) {
//            dps.setNextPaymentDate(remainingPending.get(0).getDueDate());
//        } else {
//            dps.setNextPaymentDate(null);
//            dps.setStatus(DPS.DPSStatus.MATURED);
//            dps.setMaturedDate(LocalDateTime.now());
//        }
//
//        dpsRepository.save(dps);
//
//        log.info("DPS installment paid successfully");
//
//        // Create transaction response
//        TransactionResponseDTO response = new TransactionResponseDTO();
//        response.setTransactionId(installment.getTransactionId());
//        response.setReferenceNumber(installment.getReceiptNumber());
//        response.setAmount(request.getAmount());
//        response.setTransferFee(penalty);
//        response.setTotalAmount(totalAmount);
//        response.setStatus("COMPLETED");
//        response.setDescription("DPS Installment Payment - " + installment.getInstallmentNumber());
//        response.setTimestamp(LocalDateTime.now().toString());
//
//        return response;
//    }
//
//    /**
//     * Get DPS statement with branch-level authorization
//     */
//    public DPSStatementDTO getDPSStatement(String dpsNumber, String jwtToken) {
//        log.info("Fetching DPS statement for: {}", dpsNumber);
//
//        // Fetch DPS account
//        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
//                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));
//
//        // AUTHORIZATION CHECK: Use BranchAuthorizationService
//        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: You do not have permission to view this DPS statement");
//        }
//
//        List<DPSInstallment> installments = installmentRepository.findByDpsId(dps.getId());
//
//        DPSStatementDTO statement = new DPSStatementDTO();
//        statement.setDpsNumber(dps.getDpsNumber());
//        statement.setCustomerName(dps.getCustomer().getFirstName() + " " + dps.getCustomer().getLastName());
//        statement.setMonthlyInstallment(dps.getMonthlyInstallment());
//        statement.setTotalInstallments(dps.getTenureMonths());
//        statement.setPaidInstallments(dps.getTotalInstallmentsPaid());
//        statement.setPendingInstallments(dps.getPendingInstallments());
//        statement.setTotalDeposited(dps.getTotalDeposited());
//        statement.setMaturityAmount(dps.getMaturityAmount());
//        statement.setMaturityDate(dps.getMaturityDate());
//
//        List<DPSInstallmentDTO> installmentDTOs = installments.stream()
//                .map(this::mapToInstallmentDTO)
//                .collect(Collectors.toList());
//        statement.setInstallments(installmentDTOs);
//
//        return statement;
//    }
//
//    /**
//     * Close DPS account with branch-level authorization
//     */
//    @Transactional
//    public DPSResponseDTO closeDPS(String dpsNumber, String reason, String jwtToken) {
//        log.info("Closing DPS: {}", dpsNumber);
//
//        // Fetch DPS account
//        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
//                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));
//
//        // AUTHORIZATION CHECK: Use BranchAuthorizationService
//        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: You do not have permission to close this DPS account");
//        }
//
//        if (dps.getStatus() == DPS.DPSStatus.CLOSED) {
//            throw new InvalidTransactionException("DPS is already closed");
//        }
//
//        dps.setStatus(DPS.DPSStatus.CLOSED);
//        dps.setClosedDate(LocalDateTime.now());
//        dps.setRemarks(reason != null ? reason : dps.getRemarks());
//
//        dpsRepository.save(dps);
//
//        log.info("DPS closed successfully");
//        return mapToResponseDTO(dps);
//    }
//
//    /**
//     * Mature DPS account with branch-level authorization
//     */
//    @Transactional
//    public DPSResponseDTO matureDPS(String dpsNumber, String jwtToken) {
//        log.info("Maturing DPS: {}", dpsNumber);
//
//        // Fetch DPS account
//        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
//                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));
//
//        // AUTHORIZATION CHECK: Use BranchAuthorizationService
//        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: You do not have permission to mature this DPS account");
//        }
//
//        if (dps.getPendingInstallments() > 0) {
//            throw new InvalidTransactionException("Cannot mature DPS with pending installments");
//        }
//
//        dps.setStatus(DPS.DPSStatus.MATURED);
//        dps.setMaturedDate(LocalDateTime.now());
//
//        dpsRepository.save(dps);
//
//        log.info("DPS matured successfully");
//        return mapToResponseDTO(dps);
//    }
//
//    /**
//     * Calculate maturity for DPS (no authorization required - calculation only)
//     */
//    public DPSMaturityCalculationDTO calculateMaturity(
//            BigDecimal monthlyInstallment,
//            Integer tenureMonths,
//            BigDecimal interestRate) {
//
//        BigDecimal maturityAmount = calculateMaturityAmount(monthlyInstallment, tenureMonths, interestRate);
//        BigDecimal totalDeposit = monthlyInstallment.multiply(new BigDecimal(tenureMonths));
//        BigDecimal interestEarned = maturityAmount.subtract(totalDeposit);
//
//        DPSMaturityCalculationDTO dto = new DPSMaturityCalculationDTO();
//        dto.setMonthlyInstallment(monthlyInstallment);
//        dto.setTenureMonths(tenureMonths);
//        dto.setInterestRate(interestRate);
//        dto.setTotalDeposit(totalDeposit);
//        dto.setInterestEarned(interestEarned);
//        dto.setMaturityAmount(maturityAmount);
//
//        return dto;
//    }
//
//    /**
//     * Get DPS by ID with branch-level authorization
//     */
//    public DPSResponseDTO getDPSById(Long id, String jwtToken) {
//        log.info("Fetching DPS by ID: {}", id);
//
//        // Fetch DPS account
//        DPS dps = dpsRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("DPS not found with ID: " + id));
//
//        // AUTHORIZATION CHECK: Use BranchAuthorizationService
//        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: You do not have permission to view this DPS account");
//        }
//
//        return mapToResponseDTO(dps);
//    }
//
//    /**
//     * Get DPS by number with branch-level authorization
//     */
//    public DPSResponseDTO getDPSByNumber(String dpsNumber, String jwtToken) {
//        log.info("Fetching DPS by number: {}", dpsNumber);
//
//        // Fetch DPS account
//        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
//                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));
//
//        // AUTHORIZATION CHECK: Use BranchAuthorizationService
//        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: You do not have permission to view this DPS account");
//        }
//
//        return mapToResponseDTO(dps);
//    }
//
//    /**
//     * Get all DPS accounts (Admin only - filtered by role)
//     */
//    public List<DPSResponseDTO> getAllDPS(String jwtToken) {
//        log.info("Fetching all DPS accounts");
//
//        String role = jwtUtil.extractRole(jwtToken);
//
//        // Only ADMIN can view all DPS accounts
//        if (!"ADMIN".equals(role)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: Only administrators can view all DPS accounts");
//        }
//
//        return dpsRepository.findAll().stream()
//                .map(this::mapToResponseDTO)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get DPS by customer ID with authorization
//     * ADMIN: Can view any customer's DPS
//     * CUSTOMER: Can only view their own DPS
//     * BRANCH_MANAGER: Can view DPS for customers in their branch
//     */
//    public List<DPSResponseDTO> getDPSByCustomerId(String customerId, String jwtToken) {
//        log.info("Fetching DPS for customer: {}", customerId);
//
//        String role = jwtUtil.extractRole(jwtToken);
//        String tokenCustomerId = jwtUtil.extractCustomerId(jwtToken);
//        Long userBranchId = jwtUtil.extractBranchId(jwtToken);
//
//        // Authorization check based on role
//        if ("CUSTOMER".equals(role)) {
//            if (!customerId.equals(tokenCustomerId)) {
//                throw new UnauthorizedAccessException(
//                        "Access denied: You can only view your own DPS accounts");
//            }
//        } else if ("BRANCH_MANAGER".equals(role)) {
//            // Branch Manager can only view DPS in their branch
//            // Filter will be applied after fetching
//        } else if (!"ADMIN".equals(role)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: Your role does not have permission for DPS operations");
//        }
//
//        List<DPS> dpsList = dpsRepository.findByCustomerId(customerId);
//
//        // Filter by branch for Branch Managers
//        if ("BRANCH_MANAGER".equals(role)) {
//            dpsList = dpsList.stream()
//                    .filter(dps -> dps.getBranch() != null && dps.getBranch().getId().equals(userBranchId))
//                    .collect(Collectors.toList());
//        }
//
//        return dpsList.stream()
//                .map(this::mapToResponseDTO)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get DPS by branch with authorization
//     */
//    public List<DPSResponseDTO> getDPSByBranch(Long branchId, String jwtToken) {
//        log.info("Fetching DPS for branch: {}", branchId);
//
//        String role = jwtUtil.extractRole(jwtToken);
//        Long userBranchId = jwtUtil.extractBranchId(jwtToken);
//
//        // AUTHORIZATION CHECK: Branch Managers can only view their own branch
//        if ("BRANCH_MANAGER".equals(role)) {
//            if (userBranchId == null || !branchId.equals(userBranchId)) {
//                throw new UnauthorizedAccessException(
//                        "Access denied: You can only view DPS accounts in your assigned branch");
//            }
//        } else if ("CUSTOMER".equals(role) || "LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: Your role does not have permission to view branch DPS accounts");
//        }
//        // ADMIN role proceeds without additional checks
//
//        return dpsRepository.findByBranchId(branchId).stream()
//                .map(this::mapToResponseDTO)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get DPS by status with authorization
//     */
//    public List<DPSResponseDTO> getDPSByStatus(String status, String jwtToken) {
//        log.info("Fetching DPS with status: {}", status);
//
//        String role = jwtUtil.extractRole(jwtToken);
//        Long userBranchId = jwtUtil.extractBranchId(jwtToken);
//
//        DPS.DPSStatus dpsStatus = DPS.DPSStatus.valueOf(status.toUpperCase());
//        List<DPS> dpsList = dpsRepository.findByStatus(dpsStatus);
//
//        // Filter by branch for Branch Managers
//        if ("BRANCH_MANAGER".equals(role)) {
//            dpsList = dpsList.stream()
//                    .filter(dps -> dps.getBranch() != null && dps.getBranch().getId().equals(userBranchId))
//                    .collect(Collectors.toList());
//        } else if ("CUSTOMER".equals(role) || "LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: Your role does not have permission to query DPS by status");
//        }
//        // ADMIN role gets all results without filtering
//
//        return dpsList.stream()
//                .map(this::mapToResponseDTO)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Update DPS with branch-level authorization
//     */
//    @Transactional
//    public DPSResponseDTO updateDPS(Long id, DPSUpdateRequestDTO request, String jwtToken) {
//        log.info("Updating DPS with ID: {}", id);
//
//        // Fetch DPS account
//        DPS dps = dpsRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("DPS not found with ID: " + id));
//
//        // AUTHORIZATION CHECK: Use BranchAuthorizationService
//        if (!branchAuthService.canAccessDPS(jwtToken, dps)) {
//            throw new UnauthorizedAccessException(
//                    "Access denied: You do not have permission to update this DPS account");
//        }
//
//        // Update fields if provided
//        if (request.getLinkedAccountNumber() != null) {
//            Account account = accountRepository.findByAccountNumber(request.getLinkedAccountNumber())
//                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));
//            dps.setLinkedAccount(account);
//        }
//        if (request.getAutoDebitEnabled() != null) dps.setAutoDebitEnabled(request.getAutoDebitEnabled());
//        if (request.getNomineeFirstName() != null) dps.setNomineeFirstName(request.getNomineeFirstName());
//        if (request.getNomineeLastName() != null) dps.setNomineeLastName(request.getNomineeLastName());
//        if (request.getNomineeRelationship() != null) dps.setNomineeRelationship(request.getNomineeRelationship());
//        if (request.getNomineePhone() != null) dps.setNomineePhone(request.getNomineePhone());
//        if (request.getRemarks() != null) dps.setRemarks(request.getRemarks());
//        if (request.getStatus() != null) {
//            dps.setStatus(DPS.DPSStatus.valueOf(request.getStatus().toUpperCase()));
//        }
//
//        dpsRepository.save(dps);
//        log.info("DPS updated successfully");
//        return mapToResponseDTO(dps);
//    }
//
//    // ==================== PRIVATE HELPER METHODS ====================
//
//    /**
//     * Generate installment schedule for DPS
//     */
//    private void generateInstallmentSchedule(DPS dps) {
//        List<DPSInstallment> installments = new ArrayList<>();
//        LocalDate dueDate = dps.getStartDate().plusMonths(1);
//
//        for (int i = 1; i <= dps.getTenureMonths(); i++) {
//            DPSInstallment installment = new DPSInstallment();
//            installment.setDps(dps);
//            installment.setInstallmentNumber(i);
//            installment.setDueDate(dueDate);
//            installment.setAmount(dps.getMonthlyInstallment());
//            installment.setStatus(DPSInstallment.InstallmentStatus.PENDING);
//            installments.add(installment);
//            dueDate = dueDate.plusMonths(1);
//        }
//
//        installmentRepository.saveAll(installments);
//    }
//
//    /**
//     * Calculate maturity amount using compound interest formula
//     */
//    private BigDecimal calculateMaturityAmount(BigDecimal monthlyInstallment, Integer tenureMonths, BigDecimal interestRate) {
//        // Using compound interest formula for monthly deposits
//        BigDecimal monthlyRate = interestRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
//        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
//        BigDecimal power = onePlusRate.pow(tenureMonths);
//        BigDecimal numerator = power.subtract(BigDecimal.ONE);
//        BigDecimal maturity = monthlyInstallment.multiply(numerator).divide(monthlyRate, 2, RoundingMode.HALF_UP);
//        return maturity.setScale(2, RoundingMode.HALF_UP);
//    }
//
//    /**
//     * Generate unique DPS number
//     */
//    private String generateDPSNumber() {
//        String dpsNumber;
//        do {
//            long randomNum = (long) (Math.random() * 10000000000L);
//            dpsNumber = "DPS" + randomNum;
//        } while (dpsRepository.existsByDpsNumber(dpsNumber));
//        return dpsNumber;
//    }
//
//    /**
//     * Generate unique transaction ID
//     */
//    private String generateTransactionId() {
//        return "TXN" + System.currentTimeMillis();
//    }
//
//    /**
//     * Generate unique receipt number
//     */
//    private String generateReceiptNumber() {
//        return "RCP" + System.currentTimeMillis();
//    }
//
//    /**
//     * Map DPS entity to response DTO
//     */
//    private DPSResponseDTO mapToResponseDTO(DPS dps) {
//        DPSResponseDTO dto = new DPSResponseDTO();
//        dto.setId(dps.getId());
//        dto.setDpsNumber(dps.getDpsNumber());
//        dto.setCustomerId(dps.getCustomerId());
//        dto.setCustomerName(dps.getCustomer().getFirstName() + " " + dps.getCustomer().getLastName());
//        dto.setLinkedAccountNumber(dps.getLinkedAccount() != null ? dps.getLinkedAccount().getAccountNumber() : null);
//        dto.setBranchName(dps.getBranch().getBranchName());
//        dto.setMonthlyInstallment(dps.getMonthlyInstallment());
//        dto.setTenureMonths(dps.getTenureMonths());
//        dto.setInterestRate(dps.getInterestRate());
//        dto.setMaturityAmount(dps.getMaturityAmount());
//        dto.setTotalDeposited(dps.getTotalDeposited());
//        dto.setTotalInstallmentsPaid(dps.getTotalInstallmentsPaid());
//        dto.setPendingInstallments(dps.getPendingInstallments());
//        dto.setStartDate(dps.getStartDate());
//        dto.setMaturityDate(dps.getMaturityDate());
//        dto.setNextPaymentDate(dps.getNextPaymentDate());
//        dto.setStatus(dps.getStatus().name().toLowerCase());
//        dto.setAutoDebitEnabled(dps.getAutoDebitEnabled());
//        dto.setPenaltyAmount(dps.getPenaltyAmount());
//        dto.setMissedInstallments(dps.getMissedInstallments());
//        dto.setCurrency(dps.getCurrency());
//        dto.setNomineeFirstName(dps.getNomineeFirstName());
//        dto.setNomineeLastName(dps.getNomineeLastName());
//        dto.setCreatedDate(dps.getCreatedDate());
//        return dto;
//    }
//
//    /**
//     * Map installment entity to DTO
//     */
//    private DPSInstallmentDTO mapToInstallmentDTO(DPSInstallment installment) {
//        DPSInstallmentDTO dto = new DPSInstallmentDTO();
//        dto.setInstallmentNumber(installment.getInstallmentNumber());
//        dto.setDueDate(installment.getDueDate());
//        dto.setPaymentDate(installment.getPaymentDate());
//        dto.setAmount(installment.getAmount());
//        dto.setPenaltyAmount(installment.getPenaltyAmount());
//        dto.setStatus(installment.getStatus().name().toLowerCase());
//        dto.setTransactionId(installment.getTransactionId());
//        dto.setReceiptNumber(installment.getReceiptNumber());
//        return dto;
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
//
//
//
////package com.izak.demoBankManagement.service;
////
////import com.izak.demoBankManagement.dto.*;
////import com.izak.demoBankManagement.entity.*;
////import com.izak.demoBankManagement.exception.*;
////import com.izak.demoBankManagement.repository.*;
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.stereotype.Service;
////import org.springframework.transaction.annotation.Transactional;
////
////import java.math.BigDecimal;
////import java.math.RoundingMode;
////import java.time.LocalDate;
////import java.time.LocalDateTime;
////import java.util.ArrayList;
////import java.util.List;
////import java.util.stream.Collectors;
////
////@Service
////@RequiredArgsConstructor
////@Slf4j
////public class DPSService {
////
////    private final DPSRepository dpsRepository;
////    private final DPSInstallmentRepository installmentRepository;
////    private final CustomerRepository customerRepository;
////    private final AccountRepository accountRepository;
////    private final BranchRepository branchRepository;
////    private final TransactionRepository transactionRepository;
////
////    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.02"); // 2% penalty per missed installment
////
////    @Transactional
////    public DPSResponseDTO createDPS(DPSCreateRequestDTO request) {
////        log.info("Creating new DPS for customer: {}", request.getCustomerId());
////
////        Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
////                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + request.getCustomerId()));
////
////        Branch branch = branchRepository.findByBranchCode(request.getBranchCode())
////                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchCode()));
////
////        Account linkedAccount = null;
////        if (request.getLinkedAccountNumber() != null) {
////            linkedAccount = accountRepository.findByAccountNumber(request.getLinkedAccountNumber())
////                    .orElseThrow(() -> new AccountNotFoundException("Linked account not found: " + request.getLinkedAccountNumber()));
////        }
////
////        // Calculate maturity amount and maturity date
////        BigDecimal maturityAmount = calculateMaturityAmount(
////                request.getMonthlyInstallment(),
////                request.getTenureMonths(),
////                request.getInterestRate()
////        );
////
////        LocalDate startDate = LocalDate.now();
////        LocalDate maturityDate = startDate.plusMonths(request.getTenureMonths());
////
////        DPS dps = new DPS();
////        dps.setDpsNumber(generateDPSNumber());
////        dps.setCustomer(customer);
////        dps.setCustomerId(customer.getCustomerId());
////        dps.setLinkedAccount(linkedAccount);
////        dps.setBranch(branch);
////        dps.setMonthlyInstallment(request.getMonthlyInstallment());
////        dps.setTenureMonths(request.getTenureMonths());
////        dps.setInterestRate(request.getInterestRate());
////        dps.setMaturityAmount(maturityAmount);
////        dps.setStartDate(startDate);
////        dps.setMaturityDate(maturityDate);
////        dps.setNextPaymentDate(startDate.plusMonths(1));
////        dps.setPendingInstallments(request.getTenureMonths());
////        dps.setAutoDebitEnabled(request.getAutoDebitEnabled() != null ? request.getAutoDebitEnabled() : false);
////        dps.setNomineeFirstName(request.getNomineeFirstName());
////        dps.setNomineeLastName(request.getNomineeLastName());
////        dps.setNomineeRelationship(request.getNomineeRelationship());
////        dps.setNomineePhone(request.getNomineePhone());
////        dps.setRemarks(request.getRemarks());
////        dps.setStatus(DPS.DPSStatus.ACTIVE);
////
////        dps = dpsRepository.save(dps);
////
////        // Generate installment schedule
////        generateInstallmentSchedule(dps);
////
////        log.info("DPS created successfully: {}", dps.getDpsNumber());
////        return mapToResponseDTO(dps);
////    }
////
////    @Transactional
////    public TransactionResponseDTO payInstallment(DPSPaymentRequestDTO request) {
////        log.info("Processing DPS payment for: {}", request.getDpsNumber());
////
////        DPS dps = dpsRepository.findByDpsNumber(request.getDpsNumber())
////                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + request.getDpsNumber()));
////
////        if (dps.getStatus() != DPS.DPSStatus.ACTIVE) {
////            throw new InvalidTransactionException("DPS is not active");
////        }
////
////        // Find next pending installment
////        List<DPSInstallment> pendingInstallments = installmentRepository
////                .findByDpsIdAndStatus(dps.getId(), DPSInstallment.InstallmentStatus.PENDING);
////
////        if (pendingInstallments.isEmpty()) {
////            throw new InvalidTransactionException("No pending installments");
////        }
////
////        DPSInstallment installment = pendingInstallments.get(0);
////
////        // Calculate penalty if overdue
////        BigDecimal penalty = BigDecimal.ZERO;
////        if (LocalDate.now().isAfter(installment.getDueDate())) {
////            penalty = dps.getMonthlyInstallment().multiply(PENALTY_RATE);
////            installment.setPenaltyAmount(penalty);
////        }
////
////        BigDecimal totalAmount = request.getAmount().add(penalty);
////
////        // Update installment
////        installment.setStatus(DPSInstallment.InstallmentStatus.PAID);
////        installment.setPaymentDate(LocalDate.now());
////        installment.setTotalPaid(totalAmount);
////        installment.setPaymentMode(request.getPaymentMode());
////        installment.setRemarks(request.getRemarks());
////        installment.setTransactionId(generateTransactionId());
////        installment.setReceiptNumber(generateReceiptNumber());
////        installmentRepository.save(installment);
////
////        // Update DPS
////        dps.setTotalDeposited(dps.getTotalDeposited().add(request.getAmount()));
////        dps.setTotalInstallmentsPaid(dps.getTotalInstallmentsPaid() + 1);
////        dps.setPendingInstallments(dps.getPendingInstallments() - 1);
////        dps.setLastPaymentDate(LocalDate.now());
////        dps.setPenaltyAmount(dps.getPenaltyAmount().add(penalty));
////
////        // Find next pending installment for next payment date
////        List<DPSInstallment> remainingPending = installmentRepository
////                .findByDpsIdAndStatus(dps.getId(), DPSInstallment.InstallmentStatus.PENDING);
////        if (!remainingPending.isEmpty()) {
////            dps.setNextPaymentDate(remainingPending.get(0).getDueDate());
////        } else {
////            dps.setNextPaymentDate(null);
////            dps.setStatus(DPS.DPSStatus.MATURED);
////            dps.setMaturedDate(LocalDateTime.now());
////        }
////
////        dpsRepository.save(dps);
////
////        log.info("DPS installment paid successfully");
////
////        // Create transaction response
////        TransactionResponseDTO response = new TransactionResponseDTO();
////        response.setTransactionId(installment.getTransactionId());
////        response.setReferenceNumber(installment.getReceiptNumber());
////        response.setAmount(request.getAmount());
////        response.setTransferFee(penalty);
////        response.setTotalAmount(totalAmount);
////        response.setStatus("COMPLETED");
////        response.setDescription("DPS Installment Payment - " + installment.getInstallmentNumber());
////        response.setTimestamp(LocalDateTime.now().toString());
////
////        return response;
////    }
////
////    public DPSStatementDTO getDPSStatement(String dpsNumber) {
////        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
////                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));
////
////        List<DPSInstallment> installments = installmentRepository.findByDpsId(dps.getId());
////
////        DPSStatementDTO statement = new DPSStatementDTO();
////        statement.setDpsNumber(dps.getDpsNumber());
////        statement.setCustomerName(dps.getCustomer().getFirstName() + " " + dps.getCustomer().getLastName());
////        statement.setMonthlyInstallment(dps.getMonthlyInstallment());
////        statement.setTotalInstallments(dps.getTenureMonths());
////        statement.setPaidInstallments(dps.getTotalInstallmentsPaid());
////        statement.setPendingInstallments(dps.getPendingInstallments());
////        statement.setTotalDeposited(dps.getTotalDeposited());
////        statement.setMaturityAmount(dps.getMaturityAmount());
////        statement.setMaturityDate(dps.getMaturityDate());
////
////        List<DPSInstallmentDTO> installmentDTOs = installments.stream()
////                .map(this::mapToInstallmentDTO)
////                .collect(Collectors.toList());
////        statement.setInstallments(installmentDTOs);
////
////        return statement;
////    }
////
////    @Transactional
////    public DPSResponseDTO closeDPS(String dpsNumber, String reason) {
////        log.info("Closing DPS: {}", dpsNumber);
////
////        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
////                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));
////
////        if (dps.getStatus() == DPS.DPSStatus.CLOSED) {
////            throw new InvalidTransactionException("DPS is already closed");
////        }
////
////        dps.setStatus(DPS.DPSStatus.CLOSED);
////        dps.setClosedDate(LocalDateTime.now());
////        dps.setRemarks(reason != null ? reason : dps.getRemarks());
////
////        dpsRepository.save(dps);
////
////        log.info("DPS closed successfully");
////        return mapToResponseDTO(dps);
////    }
////
////    @Transactional
////    public DPSResponseDTO matureDPS(String dpsNumber) {
////        log.info("Maturing DPS: {}", dpsNumber);
////
////        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
////                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));
////
////        if (dps.getPendingInstallments() > 0) {
////            throw new InvalidTransactionException("Cannot mature DPS with pending installments");
////        }
////
////        dps.setStatus(DPS.DPSStatus.MATURED);
////        dps.setMaturedDate(LocalDateTime.now());
////
////        dpsRepository.save(dps);
////
////        log.info("DPS matured successfully");
////        return mapToResponseDTO(dps);
////    }
////
////    public DPSMaturityCalculationDTO calculateMaturity(
////            BigDecimal monthlyInstallment,
////            Integer tenureMonths,
////            BigDecimal interestRate) {
////
////        BigDecimal maturityAmount = calculateMaturityAmount(monthlyInstallment, tenureMonths, interestRate);
////        BigDecimal totalDeposit = monthlyInstallment.multiply(new BigDecimal(tenureMonths));
////        BigDecimal interestEarned = maturityAmount.subtract(totalDeposit);
////
////        DPSMaturityCalculationDTO dto = new DPSMaturityCalculationDTO();
////        dto.setMonthlyInstallment(monthlyInstallment);
////        dto.setTenureMonths(tenureMonths);
////        dto.setInterestRate(interestRate);
////        dto.setTotalDeposit(totalDeposit);
////        dto.setInterestEarned(interestEarned);
////        dto.setMaturityAmount(maturityAmount);
////
////        return dto;
////    }
////
////    public DPSResponseDTO getDPSById(Long id) {
////        DPS dps = dpsRepository.findById(id)
////                .orElseThrow(() -> new ResourceNotFoundException("DPS not found with ID: " + id));
////        return mapToResponseDTO(dps);
////    }
////
////    public DPSResponseDTO getDPSByNumber(String dpsNumber) {
////        DPS dps = dpsRepository.findByDpsNumber(dpsNumber)
////                .orElseThrow(() -> new ResourceNotFoundException("DPS not found: " + dpsNumber));
////        return mapToResponseDTO(dps);
////    }
////
////    public List<DPSResponseDTO> getAllDPS() {
////        return dpsRepository.findAll().stream()
////                .map(this::mapToResponseDTO)
////                .collect(Collectors.toList());
////    }
////
////    public List<DPSResponseDTO> getDPSByCustomerId(String customerId) {
////        return dpsRepository.findByCustomerId(customerId).stream()
////                .map(this::mapToResponseDTO)
////                .collect(Collectors.toList());
////    }
////
////    public List<DPSResponseDTO> getDPSByBranch(Long branchId) {
////        return dpsRepository.findByBranchId(branchId).stream()
////                .map(this::mapToResponseDTO)
////                .collect(Collectors.toList());
////    }
////
////    public List<DPSResponseDTO> getDPSByStatus(String status) {
////        DPS.DPSStatus dpsStatus = DPS.DPSStatus.valueOf(status.toUpperCase());
////        return dpsRepository.findByStatus(dpsStatus).stream()
////                .map(this::mapToResponseDTO)
////                .collect(Collectors.toList());
////    }
////
////    @Transactional
////    public DPSResponseDTO updateDPS(Long id, DPSUpdateRequestDTO request) {
////        DPS dps = dpsRepository.findById(id)
////                .orElseThrow(() -> new ResourceNotFoundException("DPS not found with ID: " + id));
////
////        if (request.getLinkedAccountNumber() != null) {
////            Account account = accountRepository.findByAccountNumber(request.getLinkedAccountNumber())
////                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));
////            dps.setLinkedAccount(account);
////        }
////        if (request.getAutoDebitEnabled() != null) dps.setAutoDebitEnabled(request.getAutoDebitEnabled());
////        if (request.getNomineeFirstName() != null) dps.setNomineeFirstName(request.getNomineeFirstName());
////        if (request.getNomineeLastName() != null) dps.setNomineeLastName(request.getNomineeLastName());
////        if (request.getNomineeRelationship() != null) dps.setNomineeRelationship(request.getNomineeRelationship());
////        if (request.getNomineePhone() != null) dps.setNomineePhone(request.getNomineePhone());
////        if (request.getRemarks() != null) dps.setRemarks(request.getRemarks());
////        if (request.getStatus() != null) {
////            dps.setStatus(DPS.DPSStatus.valueOf(request.getStatus().toUpperCase()));
////        }
////
////        dpsRepository.save(dps);
////        return mapToResponseDTO(dps);
////    }
////
////    private void generateInstallmentSchedule(DPS dps) {
////        List<DPSInstallment> installments = new ArrayList<>();
////        LocalDate dueDate = dps.getStartDate().plusMonths(1);
////
////        for (int i = 1; i <= dps.getTenureMonths(); i++) {
////            DPSInstallment installment = new DPSInstallment();
////            installment.setDps(dps);
////            installment.setInstallmentNumber(i);
////            installment.setDueDate(dueDate);
////            installment.setAmount(dps.getMonthlyInstallment());
////            installment.setStatus(DPSInstallment.InstallmentStatus.PENDING);
////            installments.add(installment);
////            dueDate = dueDate.plusMonths(1);
////        }
////
////        installmentRepository.saveAll(installments);
////    }
////
////    private BigDecimal calculateMaturityAmount(BigDecimal monthlyInstallment, Integer tenureMonths, BigDecimal interestRate) {
////        // Using compound interest formula for monthly deposits
////        BigDecimal monthlyRate = interestRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);
////        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
////        BigDecimal power = onePlusRate.pow(tenureMonths);
////        BigDecimal numerator = power.subtract(BigDecimal.ONE);
////        BigDecimal maturity = monthlyInstallment.multiply(numerator).divide(monthlyRate, 2, RoundingMode.HALF_UP);
////        return maturity.setScale(2, RoundingMode.HALF_UP);
////    }
////
////    private String generateDPSNumber() {
////        String dpsNumber;
////        do {
////            long randomNum = (long) (Math.random() * 10000000000L);
////            dpsNumber = "DPS" + randomNum;
////        } while (dpsRepository.existsByDpsNumber(dpsNumber));
////        return dpsNumber;
////    }
////
////    private String generateTransactionId() {
////        return "TXN" + System.currentTimeMillis();
////    }
////
////    private String generateReceiptNumber() {
////        return "RCP" + System.currentTimeMillis();
////    }
////
////    private DPSResponseDTO mapToResponseDTO(DPS dps) {
////        DPSResponseDTO dto = new DPSResponseDTO();
////        dto.setId(dps.getId());
////        dto.setDpsNumber(dps.getDpsNumber());
////        dto.setCustomerId(dps.getCustomerId());
////        dto.setCustomerName(dps.getCustomer().getFirstName() + " " + dps.getCustomer().getLastName());
////        dto.setLinkedAccountNumber(dps.getLinkedAccount() != null ? dps.getLinkedAccount().getAccountNumber() : null);
////        dto.setBranchName(dps.getBranch().getBranchName());
////        dto.setMonthlyInstallment(dps.getMonthlyInstallment());
////        dto.setTenureMonths(dps.getTenureMonths());
////        dto.setInterestRate(dps.getInterestRate());
////        dto.setMaturityAmount(dps.getMaturityAmount());
////        dto.setTotalDeposited(dps.getTotalDeposited());
////        dto.setTotalInstallmentsPaid(dps.getTotalInstallmentsPaid());
////        dto.setPendingInstallments(dps.getPendingInstallments());
////        dto.setStartDate(dps.getStartDate());
////        dto.setMaturityDate(dps.getMaturityDate());
////        dto.setNextPaymentDate(dps.getNextPaymentDate());
////        dto.setStatus(dps.getStatus().name().toLowerCase());
////        dto.setAutoDebitEnabled(dps.getAutoDebitEnabled());
////        dto.setPenaltyAmount(dps.getPenaltyAmount());
////        dto.setMissedInstallments(dps.getMissedInstallments());
////        dto.setCurrency(dps.getCurrency());
////        dto.setNomineeFirstName(dps.getNomineeFirstName());
////        dto.setNomineeLastName(dps.getNomineeLastName());
////        dto.setCreatedDate(dps.getCreatedDate());
////        return dto;
////    }
////
////    private DPSInstallmentDTO mapToInstallmentDTO(DPSInstallment installment) {
////        DPSInstallmentDTO dto = new DPSInstallmentDTO();
////        dto.setInstallmentNumber(installment.getInstallmentNumber());
////        dto.setDueDate(installment.getDueDate());
////        dto.setPaymentDate(installment.getPaymentDate());
////        dto.setAmount(installment.getAmount());
////        dto.setPenaltyAmount(installment.getPenaltyAmount());
////        dto.setStatus(installment.getStatus().name().toLowerCase());
////        dto.setTransactionId(installment.getTransactionId());
////        dto.setReceiptNumber(installment.getReceiptNumber());
////        return dto;
////    }
////}

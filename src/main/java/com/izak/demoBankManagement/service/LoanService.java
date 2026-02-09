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
import java.security.PublicKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final LoanApprovalHistoryRepository approvalHistoryRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    private final LoanCalculationService calculationService;
    private final LoanEligibilityService eligibilityService;
    private final TransactionService transactionService;
    private final BranchAuthorizationService branchAuthorizationService;
    private final JwtUtil jwtUtil;
    // Define the missing constant here
    private static final int MAX_OVERDUE_DAYS = 90;

    // ============================================
    // LOAN RETRIEVAL
    // ============================================

    public LoanResponseDTO getLoanById(String loanId, String jwtToken) {
        Loan loan = loanRepository.findByLoanId(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));

        if (!branchAuthorizationService.canAccessLoan(jwtToken, loan)) {
            log.warn("Unauthorized access attempt to loan {} by user", loanId);
            throw new UnauthorizedAccessException("You do not have permission to access this loan.");
        }

        return mapToResponseDTO(loan);
    }

    // ============================================
    // LOAN APPLICATION
    // ============================================

    @Transactional
    public LoanResponseDTO applyForLoan(LoanApplicationRequestDTO request, String jwtToken) {
        log.info("Processing loan application for customer: {}", request.getCustomerId());

        Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + request.getCustomerId()));

        if (customer.getStatus() != Customer.Status.ACTIVE) {
            throw new LoanApplicationException("Cannot apply for loan with inactive customer account");
        }

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

        LoanEligibilityResponseDTO eligibility = eligibilityService.checkEligibility(request);
        if (!eligibility.getIsEligible()) {
            throw new LoanEligibilityException("Loan application rejected due to eligibility criteria",
                    eligibility.getReasons());
        }

        BigDecimal emi = calculationService.calculateEMI(request.getLoanAmount(), request.getAnnualInterestRate(), request.getTenureMonths());

        Loan loan = new Loan();
        loan.setLoanId(generateLoanId());
        loan.setCustomer(customer);
        loan.setAccount(account);
        loan.setLoanType(Loan.LoanType.valueOf(request.getLoanType().toUpperCase()));
        loan.setPrincipal(request.getLoanAmount());
        loan.setAnnualInterestRate(request.getAnnualInterestRate());
        loan.setTenureMonths(request.getTenureMonths());
        loan.setMonthlyEMI(emi);
        loan.setOutstandingBalance(request.getLoanAmount());
        loan.setLoanStatus(Loan.LoanStatus.APPLICATION);
        loan.setApprovalStatus(Loan.ApprovalStatus.PENDING);
        loan.setDisbursementStatus(Loan.DisbursementStatus.PENDING);
        loan.setApplicationDate(LocalDate.now());

        loan = loanRepository.save(loan);

        // FIX: Use LoanApprovalHistory.Decision.PENDING instead of DisbursementStatus
        createApprovalHistoryEntry(loan, LoanApprovalHistory.Decision.PENDING,
                LoanApprovalHistory.ApprovalStage.APPLICATION_REVIEW,
                "Loan application submitted", null, jwtToken);

        return mapToResponseDTO(loan);
    }

    // ============================================
    // LOAN APPROVAL/REJECTION
    // ============================================
    @Transactional
    public LoanResponseDTO approveLoan(LoanApprovalRequestDTO request, String jwtToken) {
        Loan loan = loanRepository.findByLoanIdWithLock(request.getLoanId())
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanId()));

        if (!branchAuthorizationService.canAccessLoan(jwtToken, loan)) {
            throw new UnauthorizedAccessException("Access denied for this branch.");
        }

        loan.setApprovalStatus(Loan.ApprovalStatus.APPROVED);
        loan.setLoanStatus(Loan.LoanStatus.APPROVED);
        loan.setApprovedDate(LocalDate.now());
        loan = loanRepository.save(loan);

        // Record history with the user performing the action
        createApprovalHistoryEntry(loan, LoanApprovalHistory.Decision.APPROVED,
                LoanApprovalHistory.ApprovalStage.FINAL_APPROVAL,
                request.getComments(), request.getApprovalConditions(), jwtToken);

        return mapToResponseDTO(loan);
    }

    @Transactional
    public LoanResponseDTO rejectLoan(LoanApprovalRequestDTO request, String jwtToken) {
        Loan loan = loanRepository.findByLoanIdWithLock(request.getLoanId())
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanId()));

        if (!branchAuthorizationService.canAccessLoan(jwtToken, loan)) {
            throw new UnauthorizedAccessException("Access denied for this branch.");
        }

        loan.setApprovalStatus(Loan.ApprovalStatus.REJECTED);
        loan.setLoanStatus(Loan.LoanStatus.APPLICATION);
        loan.setRejectionReason(request.getRejectionReason());
        loan = loanRepository.save(loan);

        createApprovalHistoryEntry(loan, LoanApprovalHistory.Decision.REJECTED,
                LoanApprovalHistory.ApprovalStage.FINAL_APPROVAL,
                request.getComments(), null, jwtToken);

        return mapToResponseDTO(loan);
    }

    // ============================================
    // LOAN DISBURSEMENT
    // ============================================

    @Transactional
    public LoanResponseDTO disburseLoan(LoanDisbursementRequestDTO request, String jwtToken) {
        Loan loan = loanRepository.findByLoanIdWithLock(request.getLoanId())
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanId()));

        if (!branchAuthorizationService.canAccessLoan(jwtToken, loan)) {
            throw new UnauthorizedAccessException("Access denied for this branch.");
        }

        DepositRequestDTO depositRequest = new DepositRequestDTO();
        depositRequest.setAccountNumber(request.getAccountNumber());
        depositRequest.setAmount(request.getDisbursementAmount());
        depositRequest.setDepositMode("NEFT");
        depositRequest.setDescription("Loan Disbursement: " + loan.getLoanId());

        // Pass token to transaction service for branch check
        transactionService.depositMoney(depositRequest, jwtToken);

        loan.setDisbursedAmount(loan.getDisbursedAmount().add(request.getDisbursementAmount()));
        loan.setDisbursementStatus(Loan.DisbursementStatus.COMPLETED);
        loan.setLoanStatus(Loan.LoanStatus.ACTIVE);

        loan.setActualDisbursementDate(LocalDate.now());


        loan = loanRepository.save(loan);

        generateRepaymentSchedule(loan);


        return mapToResponseDTO(loan);



    }


//    @Transactional
//    public LoanResponseDTO disburseLoan(LoanDisbursementRequestDTO request, String jwtToken) {
//        log.info("Processing loan disbursement for loan: {}", request.getLoanId());
//
//        Loan loan = loanRepository.findByLoanIdWithLock(request.getLoanId())
//                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanId()));
//
//        // Enforce branch-aware and role-based access
//        if (!branchAuthorizationService.canAccessLoan(jwtToken, loan)) {
//            throw new UnauthorizedAccessException("You do not have permission to disburse loans for this branch.");
//        }
//
//        if (loan.getApprovalStatus() != Loan.ApprovalStatus.APPROVED) {
//            throw new InvalidLoanStateException("Loan must be approved before disbursement");
//        }
//
//        if (loan.getDisbursementStatus() == Loan.DisbursementStatus.COMPLETED) {
//            throw new LoanAlreadyDisbursedException("Loan has already been disbursed");
//        }
//
//        // Proceed with disbursement transaction
//        DepositRequestDTO depositRequest = new DepositRequestDTO();
//        depositRequest.setAccountNumber(request.getAccountNumber());
//        depositRequest.setAmount(request.getDisbursementAmount());
//        depositRequest.setDepositMode("TRANSFER");
//        depositRequest.setDescription("Loan Disbursement - " + loan.getLoanId());
//
//        transactionService.depositMoney(depositRequest);
//
//        // Update loan state
//        loan.setDisbursedAmount(loan.getDisbursedAmount().add(request.getDisbursementAmount()));
//        loan.setDisbursementStatus(Loan.DisbursementStatus.COMPLETED);
//        loan.setLoanStatus(Loan.LoanStatus.ACTIVE);
//        loan.setActualDisbursementDate(LocalDate.now());
//
//        loan = loanRepository.save(loan);
//        return mapToResponseDTO(loan);
//    }

    // ============================================
    // HELPER METHODS (Truncated for brevity)
    // ============================================

    private void createApprovalHistoryEntry(Loan loan, LoanApprovalHistory.Decision decision,
                                            LoanApprovalHistory.ApprovalStage stage,
                                            String comments, String conditions, String jwtToken) {
        LoanApprovalHistory history = new LoanApprovalHistory();
        history.setLoan(loan);
        history.setDecision(decision);
        history.setApprovalStage(stage);
        history.setComments(comments);
        history.setApprovalConditions(conditions);
        history.setActionDate(LocalDateTime.now());

        // Handle User extraction from Token
        if (jwtToken != null) {
            String username = jwtUtil.extractUsername(jwtToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found from token"));
            history.setActionBy(user);
        } else {
            // Fallback for system-generated entries (like initial application)
            User systemUser = userRepository.findByUsername("system")
                    .orElseThrow(() -> new RuntimeException("System user not found"));
            history.setActionBy(systemUser);
        }

        approvalHistoryRepository.save(history);
    }

    private String generateLoanId() {
        return "L" + System.currentTimeMillis() + new Random().nextInt(1000);
    }

//    private LoanResponseDTO mapToResponseDTO(Loan loan) {
//        LoanResponseDTO dto = new LoanResponseDTO();
//        dto.setLoanId(loan.getLoanId());
//        dto.setLoanStatus(loan.getLoanStatus().toString());
//        dto.setPrincipal(loan.getPrincipal());
//        dto.setOutstandingBalance(loan.getOutstandingBalance());
//        return dto;
//    }
private LoanResponseDTO mapToResponseDTO(Loan loan) {
    LoanResponseDTO dto = new LoanResponseDTO();
    dto.setLoanId(loan.getLoanId());
    dto.setLoanStatus(loan.getLoanStatus().toString());
    dto.setApprovalStatus(loan.getApprovalStatus().toString());
    dto.setDisbursementStatus(loan.getDisbursementStatus().toString());
    dto.setPrincipal(loan.getPrincipal());
    dto.setOutstandingBalance(loan.getOutstandingBalance());
    dto.setAnnualInterestRate(loan.getAnnualInterestRate());
    dto.setTenureMonths(loan.getTenureMonths());
    dto.setMonthlyEMI(loan.getMonthlyEMI());
    dto.setApplicationDate(loan.getApplicationDate());
    dto.setApprovedDate(loan.getApprovedDate());
    dto.setActualDisbursementDate(loan.getActualDisbursementDate());
    dto.setDisbursedAmount(loan.getDisbursedAmount());

    // CRITICAL: Set customer information
    if (loan.getCustomer() != null) {
        dto.setCustomerId(loan.getCustomer().getCustomerId());
        dto.setCustomerName(loan.getCustomer().getFirstName() + " " +
                loan.getCustomer().getLastName());
    }

    // Set account information
    if (loan.getAccount() != null) {
        dto.setAccountNumber(loan.getAccount().getAccountNumber());
    }

    // Set optional fields
    dto.setPurpose(loan.getPurpose());
    dto.setCollateralType(loan.getCollateralType());
    dto.setCollateralValue(loan.getCollateralValue());
    dto.setApprovalConditions(loan.getApprovalConditions());
    dto.setCreditScore(loan.getCreditScore());
    dto.setEligibilityStatus(loan.getEligibilityStatus());
    dto.setLoanType(loan.getLoanType().toString());

    // Special fields for specific loan types
    dto.setLcNumber(loan.getLcNumber());
    dto.setBeneficiaryName(loan.getBeneficiaryName());
    dto.setIndustryType(loan.getIndustryType());
    dto.setBusinessTurnover(loan.getBusinessTurnover());

    return dto;
}



    /**
     * Mark loans as DEFAULTED based on overdue installments.
     * Enforces branch-aware access:
     * - ADMIN: Processes all active loans.
     * - BRANCH_MANAGER/LOAN_OFFICER: Processes only loans in their branch.
     */
    @Transactional
    public void markDefaults(String jwtToken) {
        log.info("Running default marking process triggered by user");

        String role = branchAuthorizationService.extractRole(jwtToken);
        Long tokenBranchId = branchAuthorizationService.extractBranchId(jwtToken);

        // 1. Fetch active loans
        List<Loan> activeLoans;
        if ("ADMIN".equals(role)) {
            activeLoans = loanRepository.findByLoanStatus(Loan.LoanStatus.ACTIVE);
        } else if ("BRANCH_MANAGER".equals(role) || "LOAN_OFFICER".equals(role)) {
            if (tokenBranchId == null) {
                throw new UnauthorizedAccessException("User has no assigned branch for this operation");
            }
            // Optimization: Use a repository method to filter by branch directly if available
            // For now, we filter the list using the branch check logic
            activeLoans = loanRepository.findByLoanStatus(Loan.LoanStatus.ACTIVE).stream()
                    .filter(loan -> branchAuthorizationService.canAccessLoan(jwtToken, loan))
                    .collect(Collectors.toList());
        } else {
            throw new UnauthorizedAccessException("Your role is not authorized to mark loan defaults");
        }

        LocalDate cutoffDate = LocalDate.now().minusDays(MAX_OVERDUE_DAYS);
        int markedCount = 0;

        for (Loan loan : activeLoans) {
            List<LoanRepaymentSchedule> overdueSchedules = scheduleRepository
                    .findByLoanIdAndStatus(loan.getId(), LoanRepaymentSchedule.ScheduleStatus.PENDING)
                    .stream()
                    .filter(s -> s.getDueDate().isBefore(cutoffDate))
                    .collect(Collectors.toList());

            if (!overdueSchedules.isEmpty()) {
                loan.setLoanStatus(Loan.LoanStatus.DEFAULTED);
                loan.setRemarks("Loan defaulted - " + overdueSchedules.size() +
                        " installments overdue by more than " + MAX_OVERDUE_DAYS + " days");
                loanRepository.save(loan);

                for (LoanRepaymentSchedule schedule : overdueSchedules) {
                    schedule.setStatus(LoanRepaymentSchedule.ScheduleStatus.OVERDUE);
                    scheduleRepository.save(schedule);
                }

                markedCount++;
                log.warn("Marked loan {} as DEFAULTED", loan.getLoanId());
            }
        }

        log.info("Default marking completed for role {}. Marked {} loans as defaulted", role, markedCount);
    }




    // Update these method signatures in LoanService.java:
    @Transactional(readOnly = true)
    public List<LoanListItemDTO> getLoansByCustomerId(String customerId, String jwtToken) {
        log.info("Fetching loans for customer: {}", customerId);

        // Verify customer exists
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        // Extract role and branch from token
        String role = branchAuthorizationService.extractRole(jwtToken);
        String tokenCustomerId = branchAuthorizationService.extractCustomerId(jwtToken);

        // Authorization check
        if ("CUSTOMER".equals(role)) {
            // Customers can only view their own loans
            if (!customerId.equals(tokenCustomerId)) {
                throw new UnauthorizedAccessException("You can only view your own loans");
            }
        }

        // Fetch all loans for the customer
        List<Loan> loans = loanRepository.findByCustomerCustomerId(customerId);

        // Filter by branch authorization
        return loans.stream()
                .filter(loan -> branchAuthorizationService.canAccessLoan(jwtToken, loan))
                .map(this::mapToLoanListItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanStatementResponseDTO getLoanStatement(String loanId, String jwtToken) {
        log.info("Generating loan statement for loan: {}", loanId);

        // Fetch loan with authorization check
        Loan loan = loanRepository.findByLoanId(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));

        // Check branch authorization
        if (!branchAuthorizationService.canAccessLoan(jwtToken, loan)) {
            log.warn("Unauthorized access attempt to loan statement {} by user", loanId);
            throw new UnauthorizedAccessException("You do not have permission to access this loan statement");
        }

        // Fetch repayment schedules
        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByDueDateAsc(loan.getId());

        // Calculate statement details - use totalAmount instead of getEmiAmount
        BigDecimal totalPaid = schedules.stream()
                .filter(s -> s.getStatus() == LoanRepaymentSchedule.ScheduleStatus.PAID)
                .map(LoanRepaymentSchedule::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int installmentsPaid = (int) schedules.stream()
                .filter(s -> s.getStatus() == LoanRepaymentSchedule.ScheduleStatus.PAID)
                .count();

        int installmentsPending = (int) schedules.stream()
                .filter(s -> s.getStatus() != LoanRepaymentSchedule.ScheduleStatus.PAID)
                .count();

        // Find next EMI
        LoanRepaymentSchedule nextSchedule = schedules.stream()
                .filter(s -> s.getStatus() == LoanRepaymentSchedule.ScheduleStatus.PENDING)
                .findFirst()
                .orElse(null);

        // Build response
        LoanStatementResponseDTO response = new LoanStatementResponseDTO();
        response.setLoanId(loan.getLoanId());
        response.setCustomerName(loan.getCustomer().getFirstName() + " " + loan.getCustomer().getLastName());
        response.setCustomerEmail(loan.getCustomer().getEmail());
        response.setLoanType(loan.getLoanType().toString());
        response.setPrincipal(loan.getPrincipal());
        response.setAnnualInterestRate(loan.getAnnualInterestRate());
        response.setTenureMonths(loan.getTenureMonths());
        response.setMonthlyEMI(loan.getMonthlyEMI());
        response.setApplicationDate(loan.getApplicationDate());
        response.setDisbursementDate(loan.getActualDisbursementDate());

        // FIX: Calculate total amount using EMI and months instead of principal, rate, months
        response.setTotalAmount(calculationService.calculateTotalAmount(loan.getMonthlyEMI(), loan.getTenureMonths()));

        response.setTotalPaid(totalPaid);
        response.setOutstandingBalance(loan.getOutstandingBalance());
        response.setInstallmentsPaid(installmentsPaid);
        response.setInstallmentsPending(installmentsPending);
        response.setNextEMIDate(nextSchedule != null ? nextSchedule.getDueDate() : null);
        response.setNextEMIAmount(nextSchedule != null ? nextSchedule.getTotalAmount() : null);
        response.setLoan(mapToResponseDTO(loan));



        List<RepaymentScheduleResponseDTO> scheduleDTOs = schedules.stream()
                .map(this::mapToScheduleResponseDTO)
                .collect(Collectors.toList());
        response.setRepaymentSchedule(scheduleDTOs);

        // ============ FIX: Map disbursement history to DTOs ============
        List<DisbursementHistoryDTO> disbursementDTOs = mapDisbursementHistory(loan);
        response.setDisbursementHistory(disbursementDTOs);

        return response;
    }

    /**
     * Map LoanRepaymentSchedule entity to RepaymentScheduleResponseDTO
     */
    private RepaymentScheduleResponseDTO mapToScheduleResponseDTO(LoanRepaymentSchedule schedule) {
        RepaymentScheduleResponseDTO dto = new RepaymentScheduleResponseDTO();
        dto.setInstallmentNumber(schedule.getInstallmentNumber());
        dto.setDueDate(schedule.getDueDate());
        dto.setPaymentDate(schedule.getPaymentDate());
        dto.setPrincipalAmount(schedule.getPrincipalAmount());
        dto.setInterestAmount(schedule.getInterestAmount());
        dto.setTotalAmount(schedule.getTotalAmount());
        dto.setStatus(schedule.getStatus().toString());
        dto.setBalanceAfterPayment(schedule.getBalanceAfterPayment());
        dto.setPenaltyApplied(schedule.getPenaltyApplied());
        return dto;
    }

    /**
     * Map disbursement history from Loan entity
     * If you have a LoanDisbursement entity, map from that
     * Otherwise, create a simple disbursement record from loan data
     */
    private List<DisbursementHistoryDTO> mapDisbursementHistory(Loan loan) {
        List<DisbursementHistoryDTO> history = new java.util.ArrayList<>();

        // If you have LoanDisbursement entities, use them:
        if (loan.getDisbursements() != null && !loan.getDisbursements().isEmpty()) {
            history = loan.getDisbursements().stream()
                    .map(this::mapToDisbursementHistoryDTO)
                    .collect(Collectors.toList());
        }
        // Otherwise, create a single entry from loan disbursement data
        else if (loan.getDisbursementStatus() == Loan.DisbursementStatus.COMPLETED
                && loan.getActualDisbursementDate() != null) {
            DisbursementHistoryDTO dto = new DisbursementHistoryDTO();
            dto.setDisbursementDate(loan.getActualDisbursementDate());
            dto.setAmount(loan.getDisbursedAmount());
            dto.setStatus(loan.getDisbursementStatus().toString());
            dto.setReference("Loan ID: " + loan.getLoanId());
            // Note: transactionId might not be available if not stored
            dto.setTransactionId("N/A");
            history.add(dto);
        }

        return history;
    }

    /**
     * Map LoanDisbursement entity to DisbursementHistoryDTO
     * Only needed if you have a separate LoanDisbursement entity
     */
    private DisbursementHistoryDTO mapToDisbursementHistoryDTO(LoanDisbursement disbursement) {
        DisbursementHistoryDTO dto = new DisbursementHistoryDTO();
        dto.setDisbursementDate(disbursement.getDisbursementDate());
        dto.setAmount(disbursement.getAmount());
        dto.setTransactionId(disbursement.getTransactionId());
        // FIX: Convert enum to String
        dto.setStatus(disbursement.getStatus() != null ? disbursement.getStatus().toString() : null);
        dto.setReference(disbursement.getReference());
        return dto;
    }

    @Transactional
    public TransactionResponseDTO repayLoan(LoanRepaymentRequestDTO request, String jwtToken) {
        log.info("Processing loan repayment for loan: {}", request.getLoanId());

        // Fetch and lock loan
        Loan loan = loanRepository.findByLoanIdWithLock(request.getLoanId())
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanId()));

        // Check branch authorization
        if (!branchAuthorizationService.canAccessLoan(jwtToken, loan)) {
            log.warn("Unauthorized loan repayment attempt for loan {} by user", request.getLoanId());
            throw new UnauthorizedAccessException("You do not have permission to repay this loan");
        }

        // Validate loan status
        if (loan.getLoanStatus() != Loan.LoanStatus.ACTIVE) {
            throw new LoanApplicationException("Cannot repay loan with status: " + loan.getLoanStatus());
        }

        // Validate repayment amount - use paymentAmount instead of repaymentAmount
        if (request.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new LoanApplicationException("Repayment amount must be positive");
        }

        if (request.getPaymentAmount().compareTo(loan.getOutstandingBalance()) > 0) {
            throw new LoanApplicationException("Repayment amount exceeds outstanding balance");
        }

        // Process withdrawal from account (using loan's account)
        WithdrawRequestDTO withdrawalRequest = new WithdrawRequestDTO();
        withdrawalRequest.setAccountNumber(loan.getAccount().getAccountNumber());
        withdrawalRequest.setAmount(request.getPaymentAmount());
        withdrawalRequest.setWithdrawalMode(request.getPaymentMode());
        withdrawalRequest.setDescription("Loan Repayment: " + loan.getLoanId());

        TransactionResponseDTO transactionResponse = transactionService.withdrawMoney(withdrawalRequest, jwtToken);

        // Update loan balance
        BigDecimal newBalance = loan.getOutstandingBalance().subtract(request.getPaymentAmount());
        loan.setOutstandingBalance(newBalance);

        // Check if loan is fully paid
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setLoanStatus(Loan.LoanStatus.CLOSED);
            log.info("Loan {} fully repaid and closed", loan.getLoanId());
        }

        loan = loanRepository.save(loan);

        // Update repayment schedules - use totalAmount and paymentDate
        List<LoanRepaymentSchedule> pendingSchedules = scheduleRepository
                .findByLoanIdAndStatus(loan.getId(), LoanRepaymentSchedule.ScheduleStatus.PENDING);

        BigDecimal remainingAmount = request.getPaymentAmount();
        for (LoanRepaymentSchedule schedule : pendingSchedules) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;

            if (remainingAmount.compareTo(schedule.getTotalAmount()) >= 0) {
                schedule.setStatus(LoanRepaymentSchedule.ScheduleStatus.PAID);
                schedule.setPaymentDate(request.getPaymentDate());
                schedule.setTransactionId(transactionResponse.getTransactionId());
                remainingAmount = remainingAmount.subtract(schedule.getTotalAmount());
            } else {
                // Partial payment - mark schedule but keep as pending
                schedule.setTransactionId(transactionResponse.getTransactionId());
                remainingAmount = BigDecimal.ZERO;
            }
            scheduleRepository.save(schedule);
        }

        return transactionResponse;
    }

    @Transactional(readOnly = true)
    public List<LoanListItemDTO> getPendingApprovalLoans(String jwtToken) {
        log.info("Fetching pending approval loans");

        String role = branchAuthorizationService.extractRole(jwtToken);
        Long tokenBranchId = branchAuthorizationService.extractBranchId(jwtToken);

        // Fetch pending loans
        List<Loan> pendingLoans = loanRepository.findByApprovalStatus(Loan.ApprovalStatus.PENDING);

        // Filter based on role
        if ("ADMIN".equals(role)) {
            // Admin can see all pending loans
            return pendingLoans.stream()
                    .map(this::mapToLoanListItemDTO)
                    .collect(Collectors.toList());
        } else if ("BRANCH_MANAGER".equals(role) || "LOAN_OFFICER".equals(role)) {
            // Filter by branch
            if (tokenBranchId == null) {
                throw new UnauthorizedAccessException("User has no assigned branch");
            }

            return pendingLoans.stream()
                    .filter(loan -> branchAuthorizationService.canAccessLoan(jwtToken, loan))
                    .map(this::mapToLoanListItemDTO)
                    .collect(Collectors.toList());
        } else {
            throw new UnauthorizedAccessException("Role not authorized to view pending approval loans");
        }
    }


    @Transactional(readOnly = true)
    public LoanSearchResponseDTO searchLoans(LoanSearchRequestDTO request, String jwtToken) {
        log.info("Searching loans with criteria: {}", request);

        String role = branchAuthorizationService.extractRole(jwtToken);
        Long tokenBranchId = branchAuthorizationService.extractBranchId(jwtToken);

        // Fetch all loans (in production, use proper pagination and filtering at DB level)
        List<Loan> allLoans = loanRepository.findAll();

        // Apply role-based filtering
        List<Loan> accessibleLoans;
        if ("ADMIN".equals(role)) {
            accessibleLoans = allLoans;
        } else if ("BRANCH_MANAGER".equals(role) || "LOAN_OFFICER".equals(role)) {
            if (tokenBranchId == null) {
                throw new UnauthorizedAccessException("User has no assigned branch");
            }
            accessibleLoans = allLoans.stream()
                    .filter(loan -> branchAuthorizationService.canAccessLoan(jwtToken, loan))
                    .collect(Collectors.toList());
        } else if ("CUSTOMER".equals(role)) {
            String tokenCustomerId = branchAuthorizationService.extractCustomerId(jwtToken);
            accessibleLoans = allLoans.stream()
                    .filter(loan -> loan.getCustomer().getCustomerId().equals(tokenCustomerId))
                    .collect(Collectors.toList());
        } else {
            throw new UnauthorizedAccessException("Role not authorized to search loans");
        }

        // Apply search filters
        List<LoanListItemDTO> filteredLoans = accessibleLoans.stream()
                .filter(loan -> matchesSearchCriteria(loan, request))
                .map(this::mapToLoanListItemDTO)
                .collect(Collectors.toList());

        // Apply pagination
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 0;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        int totalCount = filteredLoans.size();
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        int fromIndex = pageNumber * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalCount);

        List<LoanListItemDTO> paginatedLoans = fromIndex < totalCount ?
                filteredLoans.subList(fromIndex, toIndex) : List.of();

        // Build response
        LoanSearchResponseDTO response = new LoanSearchResponseDTO();
        response.setLoans(paginatedLoans);
        response.setTotalCount(totalCount);
        response.setPageNumber(pageNumber);
        response.setPageSize(pageSize);
        response.setTotalPages(totalPages);

        return response;
    }
    @Transactional
    public LoanResponseDTO foreCloseLoan(LoanForeclosureRequestDTO request, String jwtToken) {
        log.info("Processing loan foreclosure for loan: {}", request.getLoanId());

        // Fetch and lock loan
        Loan loan = loanRepository.findByLoanIdWithLock(request.getLoanId())
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanId()));

        // Check branch authorization
        if (!branchAuthorizationService.canAccessLoan(jwtToken, loan)) {
            log.warn("Unauthorized foreclosure attempt for loan {} by user", request.getLoanId());
            throw new UnauthorizedAccessException("You do not have permission to foreclose this loan");
        }

        // Validate loan status
        if (loan.getLoanStatus() != Loan.LoanStatus.ACTIVE) {
            throw new LoanApplicationException("Cannot foreclose loan with status: " + loan.getLoanStatus());
        }

        // Calculate foreclosure amount (outstanding balance only, no penalty field in DTO)
        BigDecimal foreclosureAmount = loan.getOutstandingBalance();

        // Process payment using loan's account
        WithdrawRequestDTO withdrawalRequest = new WithdrawRequestDTO();
        withdrawalRequest.setAccountNumber(loan.getAccount().getAccountNumber());
        withdrawalRequest.setAmount(foreclosureAmount);
        withdrawalRequest.setWithdrawalMode("TRANSFER");
        withdrawalRequest.setDescription("Loan Foreclosure: " + loan.getLoanId());

        transactionService.withdrawMoney(withdrawalRequest, jwtToken);

        // Update loan
        loan.setOutstandingBalance(BigDecimal.ZERO);
        loan.setLoanStatus(Loan.LoanStatus.CLOSED);
        loan.setRemarks("Loan foreclosed on " + LocalDate.now() + ". Foreclosure amount: " + foreclosureAmount);

        loan = loanRepository.save(loan);

        // Mark all pending schedules as paid
        List<LoanRepaymentSchedule> pendingSchedules = scheduleRepository
                .findByLoanIdAndStatus(loan.getId(), LoanRepaymentSchedule.ScheduleStatus.PENDING);

        for (LoanRepaymentSchedule schedule : pendingSchedules) {
            schedule.setStatus(LoanRepaymentSchedule.ScheduleStatus.PAID);
            schedule.setPaymentDate(LocalDate.now());
            scheduleRepository.save(schedule);
        }

        log.info("Loan {} successfully foreclosed", loan.getLoanId());
        return mapToResponseDTO(loan);
    }

    // Helper method to map Loan to LoanListItemDTO
    private LoanListItemDTO mapToLoanListItemDTO(Loan loan) {
        LoanListItemDTO dto = new LoanListItemDTO();
        dto.setLoanId(loan.getLoanId());
        dto.setLoanType(loan.getLoanType().toString());
        dto.setLoanStatus(loan.getLoanStatus().toString());
        dto.setApprovalStatus(loan.getApprovalStatus().toString());
        dto.setPrincipal(loan.getPrincipal());
        dto.setOutstandingBalance(loan.getOutstandingBalance());
        dto.setMonthlyEMI(loan.getMonthlyEMI());
        dto.setApplicationDate(loan.getApplicationDate());
        dto.setCustomerName(loan.getCustomer() != null ?
                loan.getCustomer().getFirstName() + " " + loan.getCustomer().getLastName() : null);
        dto.setCustomerId(loan.getCustomer() != null ? loan.getCustomer().getCustomerId() : null);
        return dto;
    }

    // Helper method to match search criteria - only check fields that exist in DTO
    private boolean matchesSearchCriteria(Loan loan, LoanSearchRequestDTO request) {
        if (request.getLoanType() != null && !loan.getLoanType().toString().equalsIgnoreCase(request.getLoanType())) {
            return false;
        }
        if (request.getLoanStatus() != null && !loan.getLoanStatus().toString().equalsIgnoreCase(request.getLoanStatus())) {
            return false;
        }
        if (request.getCustomerId() != null && !loan.getCustomer().getCustomerId().equals(request.getCustomerId())) {
            return false;
        }
        return true;
    }



    @Transactional(readOnly = true)
    public LoanSearchResponseDTO getAllLoans(int pageNumber, int pageSize, String jwtToken) {
        log.info("Fetching all loans - Page: {}, Size: {}", pageNumber, pageSize);

        // Extract role and branch from JWT token
        String role = branchAuthorizationService.extractRole(jwtToken);
        Long tokenBranchId = branchAuthorizationService.extractBranchId(jwtToken);

        // Fetch loans based on role
        List<Loan> allLoans;

        if ("ADMIN".equals(role)) {
            // ADMIN can see all loans system-wide
            log.debug("ADMIN role - fetching all loans");
            allLoans = loanRepository.findAll();
        } else if ("BRANCH_MANAGER".equals(role) || "LOAN_OFFICER".equals(role)) {
            // BRANCH_MANAGER and LOAN_OFFICER can only see loans in their branch
            if (tokenBranchId == null) {
                log.warn("{} has no assigned branch in token", role);
                throw new UnauthorizedAccessException("User has no assigned branch");
            }

            log.debug("{} role - fetching loans for branch ID: {}", role, tokenBranchId);
            allLoans = loanRepository.findByBranchId(tokenBranchId);
        } else {
            log.warn("Unauthorized role {} attempted to get all loans", role);
            throw new UnauthorizedAccessException("Role not authorized to view all loans");
        }

        // Apply pagination
        int adjustedPageNumber = pageNumber - 1; // Convert to 0-based index
        if (adjustedPageNumber < 0) {
            adjustedPageNumber = 0;
        }

        int totalCount = allLoans.size();
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        int fromIndex = adjustedPageNumber * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalCount);

        // Get paginated subset
        List<LoanListItemDTO> paginatedLoans = fromIndex < totalCount ?
                allLoans.subList(fromIndex, toIndex)
                        .stream()
                        .map(this::mapToLoanListItemDTO)
                        .collect(Collectors.toList())
                : List.of();

        // Build response
        LoanSearchResponseDTO response = new LoanSearchResponseDTO();
        response.setLoans(paginatedLoans);
        response.setTotalCount(totalCount);
        response.setPageNumber(adjustedPageNumber);
        response.setPageSize(pageSize);
        response.setTotalPages(totalPages);

        log.info("Retrieved {} loans out of {} total for role {}",
                paginatedLoans.size(), totalCount, role);

        return response;
    }


    private void generateRepaymentSchedule(Loan loan) {
        log.info("Generating repayment schedule for loan: {}", loan.getLoanId());

        // Clear any existing schedules (in case of regeneration)
        scheduleRepository.deleteAll(
                scheduleRepository.findByLoanIdOrderByDueDateAsc(loan.getId())
        );

        LocalDate startDate = loan.getActualDisbursementDate() != null
                ? loan.getActualDisbursementDate()
                : LocalDate.now();

        BigDecimal monthlyInterestRate = loan.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(100), 10, BigDecimal.ROUND_HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, BigDecimal.ROUND_HALF_UP);

        BigDecimal remainingPrincipal = loan.getPrincipal();

        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            LoanRepaymentSchedule schedule = new LoanRepaymentSchedule();
            schedule.setLoan(loan);
            schedule.setInstallmentNumber(i);

            // Calculate due date (first EMI due after 1 month from disbursement)
            LocalDate dueDate = startDate.plusMonths(i);
            schedule.setDueDate(dueDate);

            // Calculate interest for this period
            BigDecimal interestAmount = remainingPrincipal
                    .multiply(monthlyInterestRate)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            // Calculate principal for this period
            BigDecimal principalAmount = loan.getMonthlyEMI()
                    .subtract(interestAmount)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            // Adjust for last installment to account for rounding
            if (i == loan.getTenureMonths()) {
                principalAmount = remainingPrincipal;
                BigDecimal totalAmount = principalAmount.add(interestAmount);
                schedule.setTotalAmount(totalAmount);
            } else {
                schedule.setTotalAmount(loan.getMonthlyEMI());
            }

            schedule.setPrincipalAmount(principalAmount);
            schedule.setInterestAmount(interestAmount);
            schedule.setStatus(LoanRepaymentSchedule.ScheduleStatus.PENDING);

            // Calculate balance after this payment
            remainingPrincipal = remainingPrincipal.subtract(principalAmount);
            schedule.setBalanceAfterPayment(remainingPrincipal);

            scheduleRepository.save(schedule);
        }

        log.info("Generated {} repayment schedules for loan {}",
                loan.getTenureMonths(), loan.getLoanId());
    }



}

//    public void markDefaults(String jwtToken) {
//        // Implementation should use the system token for ADMIN-level access
//    }
//package com.izak.demoBankManagement.service;
//
//import com.izak.demoBankManagement.dto.*;
//import com.izak.demoBankManagement.entity.*;
//import com.izak.demoBankManagement.exception.*;
//import com.izak.demoBankManagement.repository.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class LoanService {
//
//    private final LoanRepository loanRepository;
//    private final LoanRepaymentScheduleRepository scheduleRepository;
//    private final LoanApprovalHistoryRepository approvalHistoryRepository;
//    private final LoanDocumentRepository documentRepository;
//    private final LoanDisbursementRepository disbursementRepository;
//    private final CustomerRepository customerRepository;
//    private final AccountRepository accountRepository;
//    private final UserRepository userRepository;
//    private final TransactionRepository transactionRepository;
//
//    private final LoanCalculationService calculationService;
//    private final LoanEligibilityService eligibilityService;
//    private final TransactionService transactionService;
//
//    private static final BigDecimal FORECLOSURE_PENALTY_RATE = new BigDecimal("2.00"); // 2%
//    private static final BigDecimal PENALTY_RATE_PER_DAY = new BigDecimal("0.05"); // 0.05% per day
//    private static final int MAX_OVERDUE_DAYS = 90;
//
//    // ============================================
//    // LOAN APPLICATION
//    // ============================================
//
//    @Transactional
//    public LoanResponseDTO applyForLoan(LoanApplicationRequestDTO request) {
//        log.info("Processing loan application for customer: {}", request.getCustomerId());
//
//        // 1. Validate customer and account
//        Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
//                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + request.getCustomerId()));
//
//        if (customer.getStatus() != Customer.Status.ACTIVE) {
//            throw new LoanApplicationException("Cannot apply for loan with inactive customer account");
//        }
//
//        if (customer.getKycStatus() != Customer.KycStatus.VERIFIED) {
//            throw new LoanApplicationException("KYC verification is required before applying for loan");
//        }
//
//        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
//                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));
//
//        if (!account.getCustomer().getId().equals(customer.getId())) {
//            throw new LoanApplicationException("Account does not belong to the customer");
//        }
//
//        if (account.getStatus() != Account.Status.ACTIVE) {
//            throw new LoanApplicationException("Cannot apply for loan with inactive account");
//        }
//
//        // 2. Check eligibility
//        LoanEligibilityResponseDTO eligibility = eligibilityService.checkEligibility(request);
//        if (!eligibility.getIsEligible()) {
//            throw new LoanEligibilityException("Loan application rejected due to eligibility criteria",
//                    eligibility.getReasons());
//        }
//
//        // 3. Calculate loan details
//        BigDecimal emi = calculationService.calculateEMI(
//                request.getLoanAmount(),
//                request.getAnnualInterestRate(),
//                request.getTenureMonths()
//        );
//
//        BigDecimal totalInterest = calculationService.calculateTotalInterest(
//                emi,
//                request.getTenureMonths(),
//                request.getLoanAmount()
//        );
//
//        BigDecimal totalAmount = calculationService.calculateTotalAmount(emi, request.getTenureMonths());
//
//        // 4. Create loan entity
//        Loan loan = new Loan();
//        loan.setLoanId(generateLoanId());
//        loan.setCustomer(customer);
//        loan.setAccount(account);
//        loan.setLoanType(Loan.LoanType.valueOf(request.getLoanType().toUpperCase()));
//        loan.setApplicantType(request.getApplicantType() != null ?
//                Loan.ApplicantType.valueOf(request.getApplicantType().toUpperCase()) :
//                Loan.ApplicantType.INDIVIDUAL);
//        loan.setPrincipal(request.getLoanAmount());
//        loan.setAnnualInterestRate(request.getAnnualInterestRate());
//        loan.setTenureMonths(request.getTenureMonths());
//        loan.setMonthlyEMI(emi);
//        loan.setTotalInterest(totalInterest);
//        loan.setTotalAmount(totalAmount);
//        loan.setOutstandingBalance(request.getLoanAmount());
//        loan.setLoanStatus(Loan.LoanStatus.APPLICATION);
//        loan.setApprovalStatus(Loan.ApprovalStatus.PENDING);
//        loan.setDisbursementStatus(Loan.DisbursementStatus.PENDING);
//        loan.setApplicationDate(LocalDate.now());
//        loan.setPurpose(request.getPurpose());
//        loan.setKycVerified(true);
//        loan.setCreditScore(eligibility.getEligibilityScore());
//        loan.setEligibilityStatus("ELIGIBLE");
//        loan.setRiskRating(eligibility.getRiskRating());
//
//        // Set collateral details for secured loans
//        if (request.getCollateralType() != null) {
//            loan.setCollateralType(request.getCollateralType());
//            loan.setCollateralValue(request.getCollateralValue());
//            loan.setCollateralDescription(request.getCollateralDescription());
//        }
//
//        // Set special fields for specific loan types
//        if (loan.getLoanType() == Loan.LoanType.IMPORT_LC_LOAN) {
//            loan.setLcNumber(request.getLcNumber());
//            loan.setBeneficiaryName(request.getBeneficiaryName());
//            loan.setBeneficiaryBank(request.getBeneficiaryBank());
//            loan.setLcExpiryDate(request.getLcExpiryDate());
//            loan.setLcAmount(request.getLcAmount());
//            loan.setPurposeOfLC(request.getPurposeOfLC());
//            loan.setPaymentTerms(request.getPaymentTerms());
//        } else if (loan.getLoanType() == Loan.LoanType.INDUSTRIAL_LOAN ||
//                loan.getLoanType() == Loan.LoanType.WORKING_CAPITAL_LOAN) {
//            loan.setIndustryType(request.getIndustryType());
//            loan.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
//            loan.setBusinessTurnover(request.getBusinessTurnover());
//        }
//
//        loan = loanRepository.save(loan);
//
//        // 5. Create approval history entry
//        createApprovalHistoryEntry(loan, Loan.ApprovalStatus.PENDING,
//                LoanApprovalHistory.ApprovalStage.APPLICATION_REVIEW,
//                "Loan application submitted", null);
//
//        log.info("Loan application created successfully: {}", loan.getLoanId());
//
//        return mapToResponseDTO(loan);
//    }
//
//    // ============================================
//    // LOAN APPROVAL/REJECTION
//    // ============================================
//
//    @Transactional
//    public LoanResponseDTO approveLoan(LoanApprovalRequestDTO request) {
//        log.info("Processing loan approval for loan: {}", request.getLoanId());
//
//        Loan loan = loanRepository.findByLoanIdWithLock(request.getLoanId())
//                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanId()));
//
//        if (loan.getApprovalStatus() != Loan.ApprovalStatus.PENDING) {
//            throw new InvalidLoanStateException("Loan is not in pending state. Current status: " +
//                    loan.getApprovalStatus());
//        }
//
//        if (!request.getApprovalStatus().equalsIgnoreCase("APPROVED")) {
//            throw new LoanApplicationException("Invalid approval status: " + request.getApprovalStatus());
//        }
//
//        // Get current user
//        User approver = getCurrentUser();
//
//        // Update loan status
//        loan.setApprovalStatus(Loan.ApprovalStatus.APPROVED);
//        loan.setLoanStatus(Loan.LoanStatus.APPROVED);
//        loan.setApprovedDate(LocalDate.now());
//        loan.setApprovedBy(approver);
//        loan.setApprovalConditions(request.getApprovalConditions());
//        loan.setApprovedAmount(loan.getPrincipal());
//
//        // Apply interest rate modification if provided
//        if (request.getInterestRateModification() != null) {
//            loan.setAnnualInterestRate(request.getInterestRateModification());
//
//            // Recalculate EMI and totals
//            BigDecimal newEMI = calculationService.calculateEMI(
//                    loan.getPrincipal(),
//                    request.getInterestRateModification(),
//                    loan.getTenureMonths()
//            );
//            loan.setMonthlyEMI(newEMI);
//            loan.setTotalInterest(calculationService.calculateTotalInterest(
//                    newEMI, loan.getTenureMonths(), loan.getPrincipal()
//            ));
//            loan.setTotalAmount(calculationService.calculateTotalAmount(newEMI, loan.getTenureMonths()));
//        }
//
//        loan = loanRepository.save(loan);
//
//        // Create approval history
//        createApprovalHistoryEntry(loan, Loan.ApprovalStatus.APPROVED,
//                LoanApprovalHistory.ApprovalStage.FINAL_APPROVAL,
//                request.getComments(), request.getApprovalConditions());
//
//        // Generate repayment schedule
//        generateRepaymentSchedule(loan);
//
//        log.info("Loan approved successfully: {}", loan.getLoanId());
//
//        return mapToResponseDTO(loan);
//    }
//
//    @Transactional
//    public LoanResponseDTO rejectLoan(LoanApprovalRequestDTO request) {
//        log.info("Processing loan rejection for loan: {}", request.getLoanId());
//
//        Loan loan = loanRepository.findByLoanIdWithLock(request.getLoanId())
//                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanId()));
//
//        if (loan.getApprovalStatus() != Loan.ApprovalStatus.PENDING) {
//            throw new InvalidLoanStateException("Loan is not in pending state. Current status: " +
//                    loan.getApprovalStatus());
//        }
//
//        if (!request.getApprovalStatus().equalsIgnoreCase("REJECTED")) {
//            throw new LoanApplicationException("Invalid approval status: " + request.getApprovalStatus());
//        }
//
//        if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
//            throw new LoanApplicationException("Rejection reason is required");
//        }
//
//        User approver = getCurrentUser();
//
//        loan.setApprovalStatus(Loan.ApprovalStatus.REJECTED);
//        loan.setLoanStatus(Loan.LoanStatus.APPLICATION);
//        loan.setRejectionReason(request.getRejectionReason());
//        loan.setApprovedBy(approver);
//
//        loan = loanRepository.save(loan);
//
//        createApprovalHistoryEntry(loan, Loan.ApprovalStatus.REJECTED,
//                LoanApprovalHistory.ApprovalStage.FINAL_APPROVAL,
//                request.getComments(), null);
//
//        log.info("Loan rejected: {}", loan.getLoanId());
//
//        return mapToResponseDTO(loan);
//    }
//
//    // ============================================
//    // LOAN DISBURSEMENT
//    // ============================================
//
//    @Transactional
//    public LoanResponseDTO disburseLoan(LoanDisbursementRequestDTO request) {
//        log.info("Processing loan disbursement for loan: {}", request.getLoanId());
//
//        Loan loan = loanRepository.findByLoanIdWithLock(request.getLoanId())
//                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanId()));
//
//        if (loan.getApprovalStatus() != Loan.ApprovalStatus.APPROVED) {
//            throw new InvalidLoanStateException("Loan must be approved before disbursement");
//        }
//
//        if (loan.getDisbursementStatus() == Loan.DisbursementStatus.COMPLETED) {
//            throw new LoanAlreadyDisbursedException("Loan has already been disbursed");
//        }
//
//        if (request.getDisbursementAmount().compareTo(loan.getPrincipal()) > 0) {
//            throw new DisbursementFailedException("Disbursement amount cannot exceed loan principal");
//        }
//
//        // Validate disbursement account
//        Account disbursementAccount = accountRepository.findByAccountNumber(request.getAccountNumber())
//                .orElseThrow(() -> new AccountNotFoundException("Disbursement account not found"));
//
//        if (!disbursementAccount.getCustomer().getId().equals(loan.getCustomer().getId())) {
//            throw new DisbursementFailedException("Disbursement account must belong to loan customer");
//        }
//
//        try {
//            // Create deposit transaction
//            DepositRequestDTO depositRequest = new DepositRequestDTO();
//            depositRequest.setAccountNumber(request.getAccountNumber());
//            depositRequest.setAmount(request.getDisbursementAmount());
//            depositRequest.setDepositMode("NEFT");
//            depositRequest.setDescription("Loan Disbursement - " + loan.getLoanId());
//            depositRequest.setRemarks("Disbursement for " + loan.getLoanType() + " loan");
//
//            TransactionResponseDTO transaction = transactionService.depositMoney(depositRequest);
//
//            // Update loan
//            loan.setDisbursedAmount(loan.getDisbursedAmount().add(request.getDisbursementAmount()));
//            loan.setDisbursementStatus(Loan.DisbursementStatus.COMPLETED);
//            loan.setLoanStatus(Loan.LoanStatus.ACTIVE);
//            loan.setActualDisbursementDate(LocalDate.now());
//            loan.setDisbursementAccountId(disbursementAccount.getId());
//
//            loan = loanRepository.save(loan);
//
//            // Create disbursement record
//            LoanDisbursement disbursement = new LoanDisbursement();
//            disbursement.setLoan(loan);
//            disbursement.setDisbursementDate(LocalDate.now());
//            disbursement.setAmount(request.getDisbursementAmount());
//            disbursement.setTransactionId(transaction.getTransactionId());
//            disbursement.setStatus(LoanDisbursement.DisbursementStatus.COMPLETED);
//            disbursement.setBankDetails(request.getBankDetails());
//            disbursement.setReference("DISB-" + loan.getLoanId());
//            disbursement.setRemarks("Loan disbursed successfully");
//
//            disbursementRepository.save(disbursement);
//
//            log.info("Loan disbursed successfully: {}", loan.getLoanId());
//
//            return mapToResponseDTO(loan);
//
//        } catch (Exception e) {
//            log.error("Disbursement failed for loan: {}", request.getLoanId(), e);
//
//            loan.setDisbursementStatus(Loan.DisbursementStatus.FAILED);
//            loanRepository.save(loan);
//
//            throw new DisbursementFailedException("Loan disbursement failed: " + e.getMessage(), e);
//        }
//    }
//
//    // ============================================
//    // LOAN REPAYMENT
//    // ============================================
//
//    @Transactional
//    public TransactionResponseDTO repayLoan(LoanRepaymentRequestDTO request) {
//        log.info("Processing loan repayment for loan: {}", request.getLoanId());
//
//        Loan loan = loanRepository.findByLoanIdWithLock(request.getLoanId())
//                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanId()));
//
//        if (loan.getLoanStatus() != Loan.LoanStatus.ACTIVE) {
//            throw new InvalidLoanStateException("Loan is not active. Current status: " + loan.getLoanStatus());
//        }
//
//        if (request.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
//            throw new InvalidTransactionException("Payment amount must be greater than zero");
//        }
//
//        // Find next pending installment
//        List<LoanRepaymentSchedule> pendingSchedules = scheduleRepository
//                .findByLoanIdAndStatus(loan.getId(), LoanRepaymentSchedule.ScheduleStatus.PENDING);
//
//        if (pendingSchedules.isEmpty()) {
//            throw new InvalidLoanStateException("No pending installments found");
//        }
//
//        LoanRepaymentSchedule schedule = pendingSchedules.get(0);
//
//        // Calculate penalty if overdue
//        BigDecimal penalty = BigDecimal.ZERO;
//        if (request.getPaymentDate().isAfter(schedule.getDueDate())) {
//            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
//                    schedule.getDueDate(), request.getPaymentDate()
//            );
//            penalty = calculationService.calculateLatePenalty(
//                    schedule.getTotalAmount(),
//                    PENALTY_RATE_PER_DAY,
//                    (int) daysOverdue
//            );
//            schedule.setPenaltyApplied(penalty);
//        }
//
//        BigDecimal totalDue = schedule.getTotalAmount().add(penalty);
//
//        if (request.getPaymentAmount().compareTo(totalDue) < 0) {
//            throw new InvalidTransactionException(
//                    String.format("Insufficient payment amount. Required: %s, Provided: %s",
//                            totalDue, request.getPaymentAmount())
//            );
//        }
//
//        try {
//            // Process withdrawal from account
//            WithdrawRequestDTO withdrawRequest = new WithdrawRequestDTO();
//            withdrawRequest.setAccountNumber(loan.getAccount().getAccountNumber());
//            withdrawRequest.setAmount(request.getPaymentAmount());
//            withdrawRequest.setWithdrawalMode(request.getPaymentMode());
//            withdrawRequest.setDescription("Loan EMI Payment - " + loan.getLoanId());
//            withdrawRequest.setRemarks("Installment #" + schedule.getInstallmentNumber());
//
//            TransactionResponseDTO transaction = transactionService.withdrawMoney(withdrawRequest);
//
//            // Update schedule
//            schedule.setStatus(LoanRepaymentSchedule.ScheduleStatus.PAID);
//            schedule.setPaymentDate(request.getPaymentDate());
//            schedule.setTransactionId(transaction.getTransactionId());
//
//            BigDecimal newOutstanding = calculationService.calculateOutstandingAfterPayment(
//                    loan.getOutstandingBalance(),
//                    schedule.getPrincipalAmount()
//            );
//            schedule.setBalanceAfterPayment(newOutstanding);
//
//            scheduleRepository.save(schedule);
//
//            // Update loan
//            loan.setOutstandingBalance(newOutstanding);
//
//            // Check if loan is fully paid
//            if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
//                loan.setLoanStatus(Loan.LoanStatus.CLOSED);
//                loan.setClosedDate(LocalDate.now());
//                log.info("Loan fully repaid and closed: {}", loan.getLoanId());
//            }
//
//            loanRepository.save(loan);
//
//            log.info("Loan repayment processed successfully");
//
//            return transaction;
//
//        } catch (Exception e) {
//            log.error("Loan repayment failed: {}", e.getMessage(), e);
//            throw new InvalidTransactionException("Loan repayment failed: " + e.getMessage());
//        }
//    }
//
//    // ============================================
//    // LOAN FORECLOSURE
//    // ============================================
//
//    @Transactional
//    public LoanResponseDTO foreCloseLoan(LoanForeclosureRequestDTO request) {
//        log.info("Processing loan foreclosure for loan: {}", request.getLoanId());
//
//        Loan loan = loanRepository.findByLoanIdWithLock(request.getLoanId())
//                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.getLoanId()));
//
//        if (loan.getLoanStatus() != Loan.LoanStatus.ACTIVE) {
//            throw new InvalidLoanStateException("Only active loans can be foreclosed");
//        }
//
//        // Calculate foreclosure amount
//        BigDecimal foreclosurePenalty = calculationService.calculatePrepaymentCharges(
//                loan.getOutstandingBalance(),
//                FORECLOSURE_PENALTY_RATE
//        );
//
//        BigDecimal totalForeclosureAmount = loan.getOutstandingBalance().add(foreclosurePenalty);
//
//        // Validate settlement account
//        Account settlementAccount = accountRepository.findByAccountNumber(request.getSettlementAccountNumber())
//                .orElseThrow(() -> new AccountNotFoundException("Settlement account not found"));
//
//        if (!settlementAccount.getCustomer().getId().equals(loan.getCustomer().getId())) {
//            throw new InvalidTransactionException("Settlement account must belong to loan customer");
//        }
//
//        if (settlementAccount.getBalance().compareTo(totalForeclosureAmount) < 0) {
//            throw new InsufficientBalanceException(
//                    String.format("Insufficient balance for foreclosure. Required: %s, Available: %s",
//                            totalForeclosureAmount, settlementAccount.getBalance())
//            );
//        }
//
//        try {
//            // Process foreclosure payment
//            WithdrawRequestDTO withdrawRequest = new WithdrawRequestDTO();
//            withdrawRequest.setAccountNumber(request.getSettlementAccountNumber());
//            withdrawRequest.setAmount(totalForeclosureAmount);
//            withdrawRequest.setWithdrawalMode("NEFT");
//            withdrawRequest.setDescription("Loan Foreclosure - " + loan.getLoanId());
//            withdrawRequest.setRemarks(String.format("Outstanding: %s, Penalty: %s",
//                    loan.getOutstandingBalance(), foreclosurePenalty));
//
//            TransactionResponseDTO transaction = transactionService.withdrawMoney(withdrawRequest);
//
//            // Update all pending schedules
//            List<LoanRepaymentSchedule> pendingSchedules = scheduleRepository
//                    .findByLoanIdAndStatus(loan.getId(), LoanRepaymentSchedule.ScheduleStatus.PENDING);
//
//            for (LoanRepaymentSchedule schedule : pendingSchedules) {
//                schedule.setStatus(LoanRepaymentSchedule.ScheduleStatus.WAIVED);
//                schedule.setPaymentDate(request.getForeclosureDate());
//                scheduleRepository.save(schedule);
//            }
//
//            // Update loan
//            loan.setOutstandingBalance(BigDecimal.ZERO);
//            loan.setLoanStatus(Loan.LoanStatus.CLOSED);
//            loan.setClosedDate(request.getForeclosureDate());
//            loan.setRemarks("Foreclosed with penalty: " + foreclosurePenalty);
//
//            loan = loanRepository.save(loan);
//
//            log.info("Loan foreclosed successfully: {}", loan.getLoanId());
//
//            return mapToResponseDTO(loan);
//
//        } catch (Exception e) {
//            log.error("Loan foreclosure failed: {}", e.getMessage(), e);
//            throw new InvalidTransactionException("Loan foreclosure failed: " + e.getMessage());
//        }
//    }
//
//    // ============================================
//    // QUERY METHODS
//    // ============================================
//
//    public LoanResponseDTO getLoanById(String loanId) {
//        Loan loan = loanRepository.findByLoanId(loanId)
//                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));
//        return mapToResponseDTO(loan);
//    }
//
//    public List<LoanListItemDTO> getLoansByCustomerId(String customerId) {
//        List<Loan> loans = loanRepository.findByCustomerId(customerId);
//        return loans.stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }
//
//    public List<LoanListItemDTO> getPendingApprovalLoans() {
//        List<Loan> loans = loanRepository.findByApprovalStatus(Loan.ApprovalStatus.PENDING);
//        return loans.stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }
//
//    public LoanSearchResponseDTO searchLoans(LoanSearchRequestDTO request) {
//        Pageable pageable = PageRequest.of(
//                request.getPageNumber(),
//                request.getPageSize(),
//                Sort.by(Sort.Direction.DESC, "applicationDate")
//        );
//
//        Loan.LoanStatus status = request.getLoanStatus() != null ?
//                Loan.LoanStatus.valueOf(request.getLoanStatus().toUpperCase()) : null;
//        Loan.LoanType loanType = request.getLoanType() != null ?
//                Loan.LoanType.valueOf(request.getLoanType().toUpperCase()) : null;
//
//        Page<Loan> page = loanRepository.searchLoans(
//                request.getCustomerId(),
//                status,
//                loanType,
//                pageable
//        );
//
//        List<LoanListItemDTO> loans = page.getContent().stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//
//        LoanSearchResponseDTO response = new LoanSearchResponseDTO();
//        response.setLoans(loans);
//        response.setTotalCount((int) page.getTotalElements());
//        response.setPageNumber(page.getNumber());
//        response.setPageSize(page.getSize());
//        response.setTotalPages(page.getTotalPages());
//
//        return response;
//    }
//
//    public LoanStatementResponseDTO getLoanStatement(String loanId) {
//        Loan loan = loanRepository.findByLoanId(loanId)
//                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));
//
//        List<LoanRepaymentSchedule> schedules = scheduleRepository
//                .findByLoanIdOrderByInstallmentNumber(loanId);
//
//        List<LoanDisbursement> disbursements = disbursementRepository.findByLoanId(loan.getId());
//
//        // Calculate statistics
//        long paidCount = schedules.stream()
//                .filter(s -> s.getStatus() == LoanRepaymentSchedule.ScheduleStatus.PAID)
//                .count();
//        long pendingCount = schedules.stream()
//                .filter(s -> s.getStatus() == LoanRepaymentSchedule.ScheduleStatus.PENDING)
//                .count();
//
//        BigDecimal totalPaid = schedules.stream()
//                .filter(s -> s.getStatus() == LoanRepaymentSchedule.ScheduleStatus.PAID)
//                .map(LoanRepaymentSchedule::getTotalAmount)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // Find next EMI
//        LoanRepaymentSchedule nextEMI = schedules.stream()
//                .filter(s -> s.getStatus() == LoanRepaymentSchedule.ScheduleStatus.PENDING)
//                .findFirst()
//                .orElse(null);
//
//        LoanStatementResponseDTO statement = new LoanStatementResponseDTO();
//        statement.setLoanId(loan.getLoanId());
//        statement.setCustomerName(loan.getCustomer().getFirstName() + " " +
//                loan.getCustomer().getLastName());
//        statement.setCustomerEmail(loan.getCustomer().getEmail());
//        statement.setLoanType(loan.getLoanType().name());
//        statement.setPrincipal(loan.getPrincipal());
//        statement.setAnnualInterestRate(loan.getAnnualInterestRate());
//        statement.setTenureMonths(loan.getTenureMonths());
//        statement.setMonthlyEMI(loan.getMonthlyEMI());
//        statement.setApplicationDate(loan.getApplicationDate());
//        statement.setDisbursementDate(loan.getActualDisbursementDate());
//        statement.setTotalAmount(loan.getTotalAmount());
//        statement.setTotalPaid(totalPaid);
//        statement.setOutstandingBalance(loan.getOutstandingBalance());
//        statement.setInstallmentsPaid((int) paidCount);
//        statement.setInstallmentsPending((int) pendingCount);
//
//        if (nextEMI != null) {
//            statement.setNextEMIDate(nextEMI.getDueDate());
//            statement.setNextEMIAmount(nextEMI.getTotalAmount());
//        }
//
//        statement.setRepaymentSchedule(schedules.stream()
//                .map(this::mapToScheduleDTO)
//                .collect(Collectors.toList()));
//
//        statement.setDisbursementHistory(disbursements.stream()
//                .map(this::mapToDisbursementDTO)
//                .collect(Collectors.toList()));
//
//        return statement;
//    }
//
//    // ============================================
//    // SCHEDULE GENERATION
//    // ============================================
//
//    private void generateRepaymentSchedule(Loan loan) {
//        log.info("Generating repayment schedule for loan: {}", loan.getLoanId());
//
//        List<LoanRepaymentSchedule> schedules = new ArrayList<>();
//        LocalDate dueDate = loan.getApprovedDate().plusMonths(1);
//        BigDecimal outstandingBalance = loan.getPrincipal();
//
//        for (int i = 1; i <= loan.getTenureMonths(); i++) {
//            BigDecimal interestAmount = calculationService.calculatePeriodInterest(
//                    outstandingBalance,
//                    loan.getAnnualInterestRate()
//            );
//
//            BigDecimal principalAmount = calculationService.calculatePeriodPrincipal(
//                    loan.getMonthlyEMI(),
//                    interestAmount
//            );
//
//            outstandingBalance = outstandingBalance.subtract(principalAmount);
//            if (outstandingBalance.compareTo(BigDecimal.ZERO) < 0) {
//                outstandingBalance = BigDecimal.ZERO;
//            }
//
//            LoanRepaymentSchedule schedule = new LoanRepaymentSchedule();
//            schedule.setLoan(loan);
//            schedule.setInstallmentNumber(i);
//            schedule.setDueDate(dueDate);
//            schedule.setPrincipalAmount(principalAmount);
//            schedule.setInterestAmount(interestAmount);
//            schedule.setTotalAmount(loan.getMonthlyEMI());
//            schedule.setBalanceAfterPayment(outstandingBalance);
//            schedule.setStatus(LoanRepaymentSchedule.ScheduleStatus.PENDING);
//
//            schedules.add(schedule);
//            dueDate = dueDate.plusMonths(1);
//        }
//
//        scheduleRepository.saveAll(schedules);
//        log.info("Generated {} repayment schedules", schedules.size());
//    }
//
//    // ============================================
//    // DEFAULT MARKING (Scheduled Task)
//    // ============================================
//
//    @Transactional
//    public void markDefaults() {
//        log.info("Running default marking process");
//
//        LocalDate cutoffDate = LocalDate.now().minusDays(MAX_OVERDUE_DAYS);
//
//        List<Loan> activeLoans = loanRepository.findByLoanStatus(Loan.LoanStatus.ACTIVE);
//
//        int markedCount = 0;
//        for (Loan loan : activeLoans) {
//            List<LoanRepaymentSchedule> overdueSchedules = scheduleRepository
//                    .findByLoanIdAndStatus(loan.getId(), LoanRepaymentSchedule.ScheduleStatus.PENDING)
//                    .stream()
//                    .filter(s -> s.getDueDate().isBefore(cutoffDate))
//                    .collect(Collectors.toList());
//
//            if (!overdueSchedules.isEmpty()) {
//                loan.setLoanStatus(Loan.LoanStatus.DEFAULTED);
//                loan.setRemarks("Loan defaulted - " + overdueSchedules.size() +
//                        " installments overdue by more than " + MAX_OVERDUE_DAYS + " days");
//                loanRepository.save(loan);
//
//                for (LoanRepaymentSchedule schedule : overdueSchedules) {
//                    schedule.setStatus(LoanRepaymentSchedule.ScheduleStatus.OVERDUE);
//                    scheduleRepository.save(schedule);
//                }
//
//                markedCount++;
//                log.warn("Marked loan {} as DEFAULTED", loan.getLoanId());
//            }
//        }
//
//        log.info("Default marking completed. Marked {} loans as defaulted", markedCount);
//    }
//
//    // ============================================
//    // HELPER METHODS
//    // ============================================
//
//    private String generateLoanId() {
//        String loanId;
//        do {
//            long randomNum = (long) (Math.random() * 10000000000L);
//            loanId = "LOAN" + randomNum;
//        } while (loanRepository.existsByLoanId(loanId));
//        return loanId;
//    }
//
//    private User getCurrentUser() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//        return userRepository.findByUsername(username)
//                .orElseThrow(() -> new InvalidCredentialsException("User not found: " + username));
//    }
//
//    private void createApprovalHistoryEntry(Loan loan, Loan.ApprovalStatus decision,
//                                            LoanApprovalHistory.ApprovalStage stage,
//                                            String comments, String conditions) {
//        User currentUser = getCurrentUser();
//
//        LoanApprovalHistory history = new LoanApprovalHistory();
//        history.setLoan(loan);
//        history.setApprovalStage(stage);
//        history.setActionDate(LocalDateTime.now());
//        history.setActionBy(currentUser);
//        history.setDecision(LoanApprovalHistory.Decision.valueOf(decision.name()));
//        history.setComments(comments);
//        history.setApprovalConditions(conditions);
//
//        approvalHistoryRepository.save(history);
//    }
//
//    // ============================================
//    // MAPPING METHODS
//    // ============================================
//
//    private LoanResponseDTO mapToResponseDTO(Loan loan) {
//        LoanResponseDTO dto = new LoanResponseDTO();
//        dto.setLoanId(loan.getLoanId());
//        dto.setLoanType(loan.getLoanType().name());
//        dto.setLoanStatus(loan.getLoanStatus().name());
//        dto.setApprovalStatus(loan.getApprovalStatus().name());
//        dto.setPrincipal(loan.getPrincipal());
//        dto.setAnnualInterestRate(loan.getAnnualInterestRate());
//        dto.setTenureMonths(loan.getTenureMonths());
//        dto.setMonthlyEMI(loan.getMonthlyEMI());
//        dto.setTotalAmount(loan.getTotalAmount());
//        dto.setTotalInterest(loan.getTotalInterest());
//        dto.setOutstandingBalance(loan.getOutstandingBalance());
//        dto.setDisbursedAmount(loan.getDisbursedAmount());
//        dto.setApprovedAmount(loan.getApprovedAmount());
//        dto.setCreditScore(loan.getCreditScore());
//        dto.setEligibilityStatus(loan.getEligibilityStatus());
//        dto.setApplicationDate(loan.getApplicationDate());
//        dto.setApprovedDate(loan.getApprovedDate());
//        dto.setActualDisbursementDate(loan.getActualDisbursementDate());
//        dto.setCustomerId(loan.getCustomer().getCustomerId());
//        dto.setCustomerName(loan.getCustomer().getFirstName() + " " +
//                loan.getCustomer().getLastName());
//        dto.setAccountNumber(loan.getAccount().getAccountNumber());
//        dto.setCollateralType(loan.getCollateralType());
//        dto.setCollateralValue(loan.getCollateralValue());
//        dto.setPurpose(loan.getPurpose());
//        dto.setApprovalConditions(loan.getApprovalConditions());
//        dto.setCreatedDate(loan.getCreatedDate());
//        dto.setDisbursementStatus(loan.getDisbursementStatus().name());
//
//        // Special fields
//        dto.setLcNumber(loan.getLcNumber());
//        dto.setBeneficiaryName(loan.getBeneficiaryName());
//        dto.setIndustryType(loan.getIndustryType());
//        dto.setBusinessTurnover(loan.getBusinessTurnover());
//
//        return dto;
//    }
//
//    private LoanListItemDTO mapToListItemDTO(Loan loan) {
//        LoanListItemDTO dto = new LoanListItemDTO();
//        dto.setLoanId(loan.getLoanId());
//        dto.setLoanType(loan.getLoanType().name());
//        dto.setLoanStatus(loan.getLoanStatus().name());
//        dto.setApprovalStatus(loan.getApprovalStatus().name());
//        dto.setPrincipal(loan.getPrincipal());
//        dto.setOutstandingBalance(loan.getOutstandingBalance());
//        dto.setMonthlyEMI(loan.getMonthlyEMI());
//        dto.setApplicationDate(loan.getApplicationDate());
//        dto.setCustomerName(loan.getCustomer().getFirstName() + " " +
//                loan.getCustomer().getLastName());
//        dto.setCustomerId(loan.getCustomer().getCustomerId());
//        return dto;
//    }
//
//    private RepaymentScheduleResponseDTO mapToScheduleDTO(LoanRepaymentSchedule schedule) {
//        RepaymentScheduleResponseDTO dto = new RepaymentScheduleResponseDTO();
//        dto.setInstallmentNumber(schedule.getInstallmentNumber());
//        dto.setDueDate(schedule.getDueDate());
//        dto.setPaymentDate(schedule.getPaymentDate());
//        dto.setPrincipalAmount(schedule.getPrincipalAmount());
//        dto.setInterestAmount(schedule.getInterestAmount());
//        dto.setTotalAmount(schedule.getTotalAmount());
//        dto.setStatus(schedule.getStatus().name());
//        dto.setBalanceAfterPayment(schedule.getBalanceAfterPayment());
//        dto.setPenaltyApplied(schedule.getPenaltyApplied());
//        return dto;
//    }
//
//    private DisbursementHistoryDTO mapToDisbursementDTO(LoanDisbursement disbursement) {
//        DisbursementHistoryDTO dto = new DisbursementHistoryDTO();
//        dto.setDisbursementDate(disbursement.getDisbursementDate());
//        dto.setAmount(disbursement.getAmount());
//        dto.setTransactionId(disbursement.getTransactionId());
//        dto.setStatus(disbursement.getStatus().name());
//        dto.setReference(disbursement.getReference());
//        return dto;
//    }
//
//
//
//    /**
//     * Get all loans with pagination
//     */
//    public LoanSearchResponseDTO getAllLoans(int pageNumber, int pageSize) {
//        log.info("Fetching all loans - Page: {}, Size: {}", pageNumber, pageSize);
//
//        Pageable pageable = PageRequest.of(
//                pageNumber,
//                pageSize,
//                Sort.by(Sort.Direction.DESC, "applicationDate")
//        );
//
//        Page<Loan> page = loanRepository.findAll(pageable);
//
//        List<LoanListItemDTO> loans = page.getContent().stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//
//        LoanSearchResponseDTO response = new LoanSearchResponseDTO();
//        response.setLoans(loans);
//        response.setTotalCount((int) page.getTotalElements());
//        response.setPageNumber(page.getNumber());
//        response.setPageSize(page.getSize());
//        response.setTotalPages(page.getTotalPages());
//
//        log.info("Retrieved {} loans out of {} total", loans.size(), page.getTotalElements());
//
//        return response;
//    }
//}
package com.izak.demoBankManagement.service;

import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.entity.Account;
import com.izak.demoBankManagement.entity.Transaction;
import com.izak.demoBankManagement.exception.*;
import com.izak.demoBankManagement.repository.AccountRepository;
import com.izak.demoBankManagement.repository.TransactionRepository;
import com.izak.demoBankManagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BranchAuthorizationService branchAuthorizationService;
    private final JwtUtil jwtUtil;

    private static final BigDecimal NEFT_FEE = new BigDecimal("2.00");
    private static final BigDecimal HIGH_PRIORITY_FEE = new BigDecimal("7.00");
    private static final BigDecimal SERVICE_TAX_RATE = new BigDecimal("0.18");
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000.00");

    @Transactional
    public TransactionResponseDTO transferFunds(TransferRequestDTO request, String authHeader) {
        log.info("Processing fund transfer from {} to {}", request.getFromAccountNumber(), request.getToAccountNumber());

        // CRITICAL FIX: Extract JWT token from Authorization header
        String jwtToken = extractToken(authHeader);

        try {
            // Extract role and user info from JWT
            String role = jwtUtil.extractRole(jwtToken);
            String customerId = jwtUtil.extractCustomerId(jwtToken);
            Long branchId = jwtUtil.extractBranchId(jwtToken);

            log.debug("User role: {}, customerId: {}, branchId: {}", role, customerId, branchId);

            // Validate accounts exist and are active
            Account fromAccount = accountRepository.findByAccountNumberWithLock(request.getFromAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + request.getFromAccountNumber()));

            Account toAccount = accountRepository.findByAccountNumberWithLock(request.getToAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + request.getToAccountNumber()));

            // Branch-aware authorization checks
            performAuthorizationCheck(role, customerId, branchId, fromAccount);

            if (fromAccount.getStatus() != Account.Status.ACTIVE) {
                throw new AccountInactiveException("Source account is not active");
            }

            if (toAccount.getStatus() != Account.Status.ACTIVE) {
                throw new AccountInactiveException("Destination account is not active");
            }

            // Calculate fees and total amount
            BigDecimal transferFee = calculateTransferFee(request);
            BigDecimal serviceTax = transferFee.multiply(SERVICE_TAX_RATE).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal totalAmount = request.getAmount().add(transferFee).add(serviceTax);

            // Check sufficient balance
            if (fromAccount.getBalance().compareTo(totalAmount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance. Available: " + fromAccount.getBalance() + ", Required: " + totalAmount);
            }

            // Perform transfer
            BigDecimal fromBalanceBefore = fromAccount.getBalance();
            fromAccount.setBalance(fromAccount.getBalance().subtract(totalAmount));
            toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            // Create transaction record
            Transaction transaction = createTransaction(request, fromAccount, toAccount,
                    transferFee, serviceTax, totalAmount, fromBalanceBefore);

            transactionRepository.save(transaction);

            log.info("Fund transfer completed successfully. Transaction ID: {}", transaction.getTransactionId());
            return mapToResponseDTO(transaction);

        } catch (Exception e) {
            log.error("Error processing transfer: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public TransactionResponseDTO depositMoney(DepositRequestDTO request, String authHeader) {
        log.info("Processing deposit to account {}", request.getAccountNumber());

        String jwtToken = extractToken(authHeader);

        try {
            String role = jwtUtil.extractRole(jwtToken);
            String customerId = jwtUtil.extractCustomerId(jwtToken);
            Long branchId = jwtUtil.extractBranchId(jwtToken);

            Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

            performAuthorizationCheck(role, customerId, branchId, account);

            if (account.getStatus() != Account.Status.ACTIVE) {
                throw new AccountInactiveException("Account is not active");
            }

            BigDecimal balanceBefore = account.getBalance();
            account.setBalance(account.getBalance().add(request.getAmount()));
            accountRepository.save(account);

            Transaction transaction = createDepositTransaction(request, account, balanceBefore);
            transactionRepository.save(transaction);

            log.info("Deposit completed successfully. Transaction ID: {}", transaction.getTransactionId());
            return mapToResponseDTO(transaction);

        } catch (Exception e) {
            log.error("Error processing deposit: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public TransactionResponseDTO withdrawMoney(WithdrawRequestDTO request, String authHeader) {
        log.info("Processing withdrawal from account {}", request.getAccountNumber());

        String jwtToken = extractToken(authHeader);

        try {
            String role = jwtUtil.extractRole(jwtToken);
            String customerId = jwtUtil.extractCustomerId(jwtToken);
            Long branchId = jwtUtil.extractBranchId(jwtToken);

            Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

            performAuthorizationCheck(role, customerId, branchId, account);

            if (account.getStatus() != Account.Status.ACTIVE) {
                throw new AccountInactiveException("Account is not active");
            }

            if (account.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient balance. Available: " + account.getBalance() + ", Requested: " + request.getAmount());
            }

            BigDecimal balanceBefore = account.getBalance();
            account.setBalance(account.getBalance().subtract(request.getAmount()));
            accountRepository.save(account);

            Transaction transaction = createWithdrawalTransaction(request, account, balanceBefore);
            transactionRepository.save(transaction);

            log.info("Withdrawal completed successfully. Transaction ID: {}", transaction.getTransactionId());
            return mapToResponseDTO(transaction);

        } catch (Exception e) {
            log.error("Error processing withdrawal: {}", e.getMessage(), e);
            throw e;
        }
    }

    public AccountBalanceDTO getAccountBalance(String accountNumber, String authHeader) {
        log.info("Fetching account balance for {}", accountNumber);

        String jwtToken = extractToken(authHeader);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
            throw new UnauthorizedAccessException("Access denied: You do not have permission to view this account's balance");
        }

        AccountBalanceDTO dto = new AccountBalanceDTO();
        dto.setAccountNumber(account.getAccountNumber());
        dto.setCustomerId(account.getCustomer().getCustomerId());
        dto.setAccountType(account.getAccountType());

        if (account.getBranch() != null) {
            dto.setBranchCode(account.getBranch().getBranchCode());
            dto.setBranchName(account.getBranch().getBranchName());
        }

        dto.setBalance(account.getBalance());
        dto.setCurrency(account.getCurrency());
        dto.setStatus(account.getStatus().name().toLowerCase());

        log.info("Account balance retrieved successfully for {}", accountNumber);
        return dto;
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Extract JWT token from Authorization header
     * Handles both "Bearer token" and "token" formats
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            throw new UnauthorizedAccessException("Authorization header is missing");
        }

        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return authHeader;
    }

    /**
     * Perform role-based authorization check
     */
    private void performAuthorizationCheck(String role, String customerId, Long branchId, Account account) {
        if ("ADMIN".equals(role)) {
            log.debug("Admin performing operation - access granted");
            return;
        }

        if ("BRANCH_MANAGER".equals(role)) {
            if (branchId == null) {
                throw new UnauthorizedAccessException("Branch Manager has no assigned branch");
            }
            Long accountBranchId = account.getBranch() != null ? account.getBranch().getId() : null;
            if (!branchId.equals(accountBranchId)) {
                throw new UnauthorizedAccessException("Access denied: You can only perform transactions for accounts in your branch");
            }
            log.debug("Branch Manager (branch {}) authorized", branchId);
            return;
        }

        if ("CUSTOMER".equals(role)) {
            if (customerId == null || !customerId.equals(account.getCustomerId())) {
                throw new UnauthorizedAccessException("Access denied: You can only perform transactions on your own accounts");
            }
            log.debug("Customer {} authorized", customerId);
            return;
        }

        if ("LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
            throw new UnauthorizedAccessException("Access denied: " + role + " is not authorized to perform transactions");
        }

        throw new UnauthorizedAccessException("Access denied: Unknown role");
    }

    private Transaction createTransaction(TransferRequestDTO request, Account fromAccount,
                                          Account toAccount, BigDecimal transferFee,
                                          BigDecimal serviceTax, BigDecimal totalAmount,
                                          BigDecimal fromBalanceBefore) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setReferenceNumber(generateReferenceNumber());
        transaction.setFromAccount(fromAccount);
        transaction.setFromAccountNumber(fromAccount.getAccountNumber());
        transaction.setToAccount(toAccount);
        transaction.setToAccountNumber(toAccount.getAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency("USD");
        transaction.setTransferFee(transferFee);
        transaction.setServiceTax(serviceTax);
        transaction.setTotalAmount(totalAmount);
        transaction.setTransferMode(Transaction.TransferMode.valueOf(request.getTransferMode().toUpperCase()));
        transaction.setTransactionType(Transaction.TransactionType.TRANSFER);
        transaction.setTransferType(determineTransferType(request.getTransferType()));
        transaction.setStatus(Transaction.Status.COMPLETED);
        transaction.setDescription(request.getDescription());
        transaction.setRemarks(request.getRemarks());
        transaction.setBalanceBefore(fromBalanceBefore);
        transaction.setBalanceAfter(fromAccount.getBalance());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setReceiptNumber(generateReceiptNumber());
        transaction.setVerified(true);
        transaction.setFraudCheckPassed(true);
        transaction.setRequiresApproval(request.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0);
        transaction.setBeneficiaryName(toAccount.getCustomer().getFirstName() + " " + toAccount.getCustomer().getLastName());
        transaction.setBeneficiaryBank("Same Bank");
        return transaction;
    }

    private Transaction createDepositTransaction(DepositRequestDTO request, Account account, BigDecimal balanceBefore) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setReferenceNumber(generateReferenceNumber());
        transaction.setFromAccount(null);
        transaction.setFromAccountNumber("CASH_DEPOSIT");
        transaction.setToAccount(account);
        transaction.setToAccountNumber(account.getAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency("USD");
        transaction.setTransferFee(BigDecimal.ZERO);
        transaction.setServiceTax(BigDecimal.ZERO);
        transaction.setTotalAmount(request.getAmount());
        transaction.setTransferMode(Transaction.TransferMode.valueOf(request.getDepositMode().toUpperCase()));
        transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
        transaction.setStatus(Transaction.Status.COMPLETED);
        transaction.setDescription(request.getDescription());
        transaction.setRemarks(request.getRemarks());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(account.getBalance());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setReceiptNumber(generateReceiptNumber());
        transaction.setVerified(true);
        transaction.setFraudCheckPassed(true);
        transaction.setRequiresApproval(false);
        return transaction;
    }

    private Transaction createWithdrawalTransaction(WithdrawRequestDTO request, Account account, BigDecimal balanceBefore) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setReferenceNumber(generateReferenceNumber());
        transaction.setFromAccount(account);
        transaction.setFromAccountNumber(account.getAccountNumber());
        transaction.setToAccount(null);
        transaction.setToAccountNumber("CASH_WITHDRAWAL");
        transaction.setAmount(request.getAmount());
        transaction.setCurrency("USD");
        transaction.setTransferFee(BigDecimal.ZERO);
        transaction.setServiceTax(BigDecimal.ZERO);
        transaction.setTotalAmount(request.getAmount());
        transaction.setTransferMode(Transaction.TransferMode.valueOf(request.getWithdrawalMode().toUpperCase()));
        transaction.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
        transaction.setStatus(Transaction.Status.COMPLETED);
        transaction.setDescription(request.getDescription());
        transaction.setRemarks(request.getRemarks());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(account.getBalance());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setReceiptNumber(generateReceiptNumber());
        transaction.setVerified(true);
        transaction.setFraudCheckPassed(true);
        transaction.setRequiresApproval(false);
        return transaction;
    }

    private BigDecimal calculateTransferFee(TransferRequestDTO request) {
        if ("high".equalsIgnoreCase(request.getPriority())) {
            return HIGH_PRIORITY_FEE;
        }
        if ("own".equalsIgnoreCase(request.getTransferType())) {
            return BigDecimal.ZERO;
        }
        return NEFT_FEE;
    }

    private Transaction.TransferType determineTransferType(String type) {
        if (type == null) return Transaction.TransferType.OTHER;
        return "own".equalsIgnoreCase(type) ? Transaction.TransferType.OWN : Transaction.TransferType.OTHER;
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis();
    }

    private String generateReferenceNumber() {
        return "REF" + System.currentTimeMillis();
    }

    private String generateReceiptNumber() {
        return "RCP" + System.currentTimeMillis();
    }

    private TransactionResponseDTO mapToResponseDTO(Transaction transaction) {
        TransactionResponseDTO dto = new TransactionResponseDTO();
        dto.setTransactionId(transaction.getTransactionId());
        dto.setReferenceNumber(transaction.getReferenceNumber());
        dto.setFromAccountNumber(transaction.getFromAccountNumber());
        dto.setToAccountNumber(transaction.getToAccountNumber());

        if (transaction.getFromAccount() != null && transaction.getFromAccount().getBranch() != null) {
            dto.setFromBranchCode(transaction.getFromAccount().getBranch().getBranchCode());
            dto.setFromBranchName(transaction.getFromAccount().getBranch().getBranchName());
        }

        if (transaction.getToAccount() != null && transaction.getToAccount().getBranch() != null) {
            dto.setToBranchCode(transaction.getToAccount().getBranch().getBranchCode());
            dto.setToBranchName(transaction.getToAccount().getBranch().getBranchName());
        }

        dto.setAmount(transaction.getAmount());
        dto.setCurrency(transaction.getCurrency());
        dto.setTransferFee(transaction.getTransferFee());
        dto.setServiceTax(transaction.getServiceTax());
        dto.setTotalAmount(transaction.getTotalAmount());
        dto.setTransferMode(transaction.getTransferMode().name());
        dto.setTransactionType(transaction.getTransactionType().name());
        dto.setStatus(transaction.getStatus().name());
        dto.setDescription(transaction.getDescription());
        dto.setRemarks(transaction.getRemarks());
        dto.setTimestamp(transaction.getTimestamp().toString());
        dto.setCompletedAt(transaction.getCompletedAt() != null ? transaction.getCompletedAt().toString() : null);
        dto.setReceiptNumber(transaction.getReceiptNumber());
        dto.setBalanceBefore(transaction.getBalanceBefore());
        dto.setBalanceAfter(transaction.getBalanceAfter());
        dto.setBeneficiaryName(transaction.getBeneficiaryName());
        dto.setBeneficiaryBank(transaction.getBeneficiaryBank());
        dto.setFraudCheckPassed(transaction.getFraudCheckPassed());
        dto.setRequiresApproval(transaction.getRequiresApproval());

        return dto;
    }
}


//package com.izak.demoBankManagement.service;
//
//import com.izak.demoBankManagement.dto.*;
//import com.izak.demoBankManagement.entity.Account;
//import com.izak.demoBankManagement.entity.Transaction;
//import com.izak.demoBankManagement.exception.*;
//import com.izak.demoBankManagement.repository.AccountRepository;
//import com.izak.demoBankManagement.repository.TransactionRepository;
//import com.izak.demoBankManagement.security.JwtUtil;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class TransactionService {
//    private final AccountRepository accountRepository;
//    private final TransactionRepository transactionRepository;
//    private final BranchAuthorizationService branchAuthorizationService;
//    private final JwtUtil jwtUtil;
//
//    private static final BigDecimal NEFT_FEE = new BigDecimal("2.00");
//    private static final BigDecimal HIGH_PRIORITY_FEE = new BigDecimal("7.00");
//    private static final BigDecimal SERVICE_TAX_RATE = new BigDecimal("0.18"); // 18%
//    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000.00");
//
//    // ============================================
//    // FUND TRANSFER (Account to Account)
//    // ============================================
//
//    @Transactional
//    public TransactionResponseDTO transferFunds(TransferRequestDTO request, String jwtToken) {
//        log.info("Processing fund transfer from {} to {}", request.getFromAccountNumber(), request.getToAccountNumber());
//
//        // 1. Extract role and perform authorization checks
//        String role = jwtUtil.extractRole(jwtToken);
//        String customerId = jwtUtil.extractCustomerId(jwtToken);
//        Long branchId = jwtUtil.extractBranchId(jwtToken);
//
//        // 2. Validate accounts exist and are active
//        Account fromAccount = accountRepository.findByAccountNumberWithLock(request.getFromAccountNumber())
//                .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + request.getFromAccountNumber()));
//
//        Account toAccount = accountRepository.findByAccountNumberWithLock(request.getToAccountNumber())
//                .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + request.getToAccountNumber()));
//
//        // 3. Branch-aware authorization checks
//        if ("ADMIN".equals(role)) {
//            // ADMIN has system-wide access - allow all transfers
//            log.debug("Admin performing transfer - access granted");
//        } else if ("BRANCH_MANAGER".equals(role)) {
//            // BRANCH_MANAGER can only transfer from accounts in their branch
//            if (branchId == null) {
//                throw new UnauthorizedAccessException("Branch Manager has no assigned branch");
//            }
//            Long fromAccountBranchId = fromAccount.getBranch() != null ? fromAccount.getBranch().getId() : null;
//            if (!branchId.equals(fromAccountBranchId)) {
//                throw new UnauthorizedAccessException("Access denied: You can only perform transactions for accounts in your branch");
//            }
//            log.debug("Branch Manager (branch {}) authorized to transfer from account in branch {}", branchId, fromAccountBranchId);
//        } else if ("CUSTOMER".equals(role)) {
//            // CUSTOMER can only transfer from their own accounts
//            if (customerId == null || !customerId.equals(fromAccount.getCustomerId())) {
//                throw new UnauthorizedAccessException("Access denied: You can only transfer from your own accounts");
//            }
//            log.debug("Customer {} authorized to transfer from their account", customerId);
//        } else if ("LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
//            // LOAN_OFFICER and CARD_OFFICER are not authorized for transactions
//            throw new UnauthorizedAccessException("Access denied: " + role + " is not authorized to perform transactions");
//        } else {
//            throw new UnauthorizedAccessException("Access denied: Unknown role");
//        }
//
//        if (fromAccount.getStatus() != Account.Status.ACTIVE) {
//            throw new AccountInactiveException("Source account is not active");
//        }
//
//        if (toAccount.getStatus() != Account.Status.ACTIVE) {
//            throw new AccountInactiveException("Destination account is not active");
//        }
//
//        // 4. Calculate fees and total amount
//        BigDecimal transferFee = calculateTransferFee(request);
//        BigDecimal serviceTax = transferFee.multiply(SERVICE_TAX_RATE).setScale(2, BigDecimal.ROUND_HALF_UP);
//        BigDecimal totalAmount = request.getAmount().add(transferFee).add(serviceTax);
//
//        // 5. Check sufficient balance
//        if (fromAccount.getBalance().compareTo(totalAmount) < 0) {
//            throw new InsufficientBalanceException("Insufficient balance. Available: " + fromAccount.getBalance() + ", Required: " + totalAmount);
//        }
//
//        // 6. Perform transfer (debit from source, credit to destination)
//        BigDecimal fromBalanceBefore = fromAccount.getBalance();
//
//        fromAccount.setBalance(fromAccount.getBalance().subtract(totalAmount));
//        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
//
//        accountRepository.save(fromAccount);
//        accountRepository.save(toAccount);
//
//        // 7. Create transaction record
//        Transaction transaction = new Transaction();
//        transaction.setTransactionId(generateTransactionId());
//        transaction.setReferenceNumber(generateReferenceNumber());
//        transaction.setFromAccount(fromAccount);
//        transaction.setFromAccountNumber(fromAccount.getAccountNumber());
//        transaction.setToAccount(toAccount);
//        transaction.setToAccountNumber(toAccount.getAccountNumber());
//        transaction.setAmount(request.getAmount());
//        transaction.setCurrency("USD");
//        transaction.setTransferFee(transferFee);
//        transaction.setServiceTax(serviceTax);
//        transaction.setTotalAmount(totalAmount);
//        transaction.setTransferMode(Transaction.TransferMode.valueOf(request.getTransferMode().toUpperCase()));
//        transaction.setTransactionType(Transaction.TransactionType.TRANSFER);
//        transaction.setTransferType(determineTransferType(request.getTransferType()));
//        transaction.setStatus(Transaction.Status.COMPLETED);
//        transaction.setDescription(request.getDescription());
//        transaction.setRemarks(request.getRemarks());
//        transaction.setBalanceBefore(fromBalanceBefore);
//        transaction.setBalanceAfter(fromAccount.getBalance());
//        transaction.setTimestamp(LocalDateTime.now());
//        transaction.setCompletedAt(LocalDateTime.now());
//        transaction.setReceiptNumber(generateReceiptNumber());
//        transaction.setVerified(true);
//        transaction.setFraudCheckPassed(true);
//        transaction.setRequiresApproval(request.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0);
//        transaction.setBeneficiaryName(toAccount.getCustomer().getFirstName() + " " + toAccount.getCustomer().getLastName());
//        transaction.setBeneficiaryBank("Same Bank");
//
//        transactionRepository.save(transaction);
//
//        log.info("Fund transfer completed successfully. Transaction ID: {}", transaction.getTransactionId());
//
//        // 8. Return response DTO
//        return mapToResponseDTO(transaction);
//    }
//
//    // ============================================
//    // DEPOSIT MONEY
//    // ============================================
//    @Transactional
//    public TransactionResponseDTO depositMoney(DepositRequestDTO request, String jwtToken) {
//        log.info("Processing deposit to account {}", request.getAccountNumber());
//
//        // 1. Extract role and perform authorization checks
//        String role = jwtUtil.extractRole(jwtToken);
//        String customerId = jwtUtil.extractCustomerId(jwtToken);
//        Long branchId = jwtUtil.extractBranchId(jwtToken);
//
//        // 2. Validate account
//        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
//                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));
//
//        // 3. Branch-aware authorization checks
//        if ("ADMIN".equals(role)) {
//            // ADMIN has system-wide access
//            log.debug("Admin performing deposit - access granted");
//        } else if ("BRANCH_MANAGER".equals(role)) {
//            // BRANCH_MANAGER can only deposit to accounts in their branch
//            if (branchId == null) {
//                throw new UnauthorizedAccessException("Branch Manager has no assigned branch");
//            }
//            Long accountBranchId = account.getBranch() != null ? account.getBranch().getId() : null;
//            if (!branchId.equals(accountBranchId)) {
//                throw new UnauthorizedAccessException("Access denied: You can only perform transactions for accounts in your branch");
//            }
//            log.debug("Branch Manager (branch {}) authorized to deposit to account in branch {}", branchId, accountBranchId);
//        } else if ("CUSTOMER".equals(role)) {
//            // CUSTOMER can only deposit to their own accounts
//            if (customerId == null || !customerId.equals(account.getCustomerId())) {
//                throw new UnauthorizedAccessException("Access denied: You can only deposit to your own accounts");
//            }
//            log.debug("Customer {} authorized to deposit to their account", customerId);
//        } else if ("LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
//            // LOAN_OFFICER and CARD_OFFICER are not authorized for transactions
//            throw new UnauthorizedAccessException("Access denied: " + role + " is not authorized to perform transactions");
//        } else {
//            throw new UnauthorizedAccessException("Access denied: Unknown role");
//        }
//
//        if (account.getStatus() != Account.Status.ACTIVE) {
//            throw new AccountInactiveException("Account is not active");
//        }
//
//        // 4. Update balance (no fees for deposits)
//        BigDecimal balanceBefore = account.getBalance();
//        account.setBalance(account.getBalance().add(request.getAmount()));
//        accountRepository.save(account);
//
//        // 5. Create transaction record
//        Transaction transaction = new Transaction();
//        transaction.setTransactionId(generateTransactionId());
//        transaction.setReferenceNumber(generateReferenceNumber());
//        transaction.setFromAccount(null); // No source account for deposits
//        transaction.setFromAccountNumber("CASH_DEPOSIT");
//        transaction.setToAccount(account);
//        transaction.setToAccountNumber(account.getAccountNumber());
//        transaction.setAmount(request.getAmount());
//        transaction.setCurrency("USD");
//        transaction.setTransferFee(BigDecimal.ZERO);
//        transaction.setServiceTax(BigDecimal.ZERO);
//        transaction.setTotalAmount(request.getAmount());
//        transaction.setTransferMode(Transaction.TransferMode.valueOf(request.getDepositMode().toUpperCase()));
//        transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
//        transaction.setStatus(Transaction.Status.COMPLETED);
//        transaction.setDescription(request.getDescription());
//        transaction.setRemarks(request.getRemarks());
//        transaction.setBalanceBefore(balanceBefore);
//        transaction.setBalanceAfter(account.getBalance());
//        transaction.setTimestamp(LocalDateTime.now());
//        transaction.setCompletedAt(LocalDateTime.now());
//        transaction.setReceiptNumber(generateReceiptNumber());
//        transaction.setVerified(true);
//        transaction.setFraudCheckPassed(true);
//        transaction.setRequiresApproval(false);
//
//        transactionRepository.save(transaction);
//
//        log.info("Deposit completed successfully. Transaction ID: {}", transaction.getTransactionId());
//
//        return mapToResponseDTO(transaction);
//    }
//
//    // ============================================
//    // WITHDRAW MONEY
//    // ============================================
//    @Transactional
//    public TransactionResponseDTO withdrawMoney(WithdrawRequestDTO request, String jwtToken) {
//        log.info("Processing withdrawal from account {}", request.getAccountNumber());
//
//        // 1. Extract role and perform authorization checks
//        String role = jwtUtil.extractRole(jwtToken);
//        String customerId = jwtUtil.extractCustomerId(jwtToken);
//        Long branchId = jwtUtil.extractBranchId(jwtToken);
//
//        // 2. Validate account
//        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
//                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));
//
//        // 3. Branch-aware authorization checks
//        if ("ADMIN".equals(role)) {
//            // ADMIN has system-wide access
//            log.debug("Admin performing withdrawal - access granted");
//        } else if ("BRANCH_MANAGER".equals(role)) {
//            // BRANCH_MANAGER can only withdraw from accounts in their branch
//            if (branchId == null) {
//                throw new UnauthorizedAccessException("Branch Manager has no assigned branch");
//            }
//            Long accountBranchId = account.getBranch() != null ? account.getBranch().getId() : null;
//            if (!branchId.equals(accountBranchId)) {
//                throw new UnauthorizedAccessException("Access denied: You can only perform transactions for accounts in your branch");
//            }
//            log.debug("Branch Manager (branch {}) authorized to withdraw from account in branch {}", branchId, accountBranchId);
//        } else if ("CUSTOMER".equals(role)) {
//            // CUSTOMER can only withdraw from their own accounts
//            if (customerId == null || !customerId.equals(account.getCustomerId())) {
//                throw new UnauthorizedAccessException("Access denied: You can only withdraw from your own accounts");
//            }
//            log.debug("Customer {} authorized to withdraw from their account", customerId);
//        } else if ("LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
//            // LOAN_OFFICER and CARD_OFFICER are not authorized for transactions
//            throw new UnauthorizedAccessException("Access denied: " + role + " is not authorized to perform transactions");
//        } else {
//            throw new UnauthorizedAccessException("Access denied: Unknown role");
//        }
//
//        if (account.getStatus() != Account.Status.ACTIVE) {
//            throw new AccountInactiveException("Account is not active");
//        }
//
//        // 4. Check sufficient balance (no fees for withdrawals)
//        if (account.getBalance().compareTo(request.getAmount()) < 0) {
//            throw new InsufficientBalanceException("Insufficient balance. Available: " + account.getBalance() + ", Requested: " + request.getAmount());
//        }
//
//        // 5. Update balance
//        BigDecimal balanceBefore = account.getBalance();
//        account.setBalance(account.getBalance().subtract(request.getAmount()));
//        accountRepository.save(account);
//
//        // 6. Create transaction record
//        Transaction transaction = new Transaction();
//        transaction.setTransactionId(generateTransactionId());
//        transaction.setReferenceNumber(generateReferenceNumber());
//        transaction.setFromAccount(account);
//        transaction.setFromAccountNumber(account.getAccountNumber());
//        transaction.setToAccount(null); // No destination account for withdrawals
//        transaction.setToAccountNumber("CASH_WITHDRAWAL");
//        transaction.setAmount(request.getAmount());
//        transaction.setCurrency("USD");
//        transaction.setTransferFee(BigDecimal.ZERO);
//        transaction.setServiceTax(BigDecimal.ZERO);
//        transaction.setTotalAmount(request.getAmount());
//        transaction.setTransferMode(Transaction.TransferMode.valueOf(request.getWithdrawalMode().toUpperCase()));
//        transaction.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
//        transaction.setStatus(Transaction.Status.COMPLETED);
//        transaction.setDescription(request.getDescription());
//        transaction.setRemarks(request.getRemarks());
//        transaction.setBalanceBefore(balanceBefore);
//        transaction.setBalanceAfter(account.getBalance());
//        transaction.setTimestamp(LocalDateTime.now());
//        transaction.setCompletedAt(LocalDateTime.now());
//        transaction.setReceiptNumber(generateReceiptNumber());
//        transaction.setVerified(true);
//        transaction.setFraudCheckPassed(true);
//        transaction.setRequiresApproval(false);
//
//        transactionRepository.save(transaction);
//
//        log.info("Withdrawal completed successfully. Transaction ID: {}", transaction.getTransactionId());
//
//        return mapToResponseDTO(transaction);
//    }
//
//    // ============================================
//    // GET ACCOUNT BALANCE
//    // ============================================
//
//    public AccountBalanceDTO getAccountBalance(String accountNumber, String jwtToken) {
//        log.info("Fetching account balance for {}", accountNumber);
//
//        // 1. Validate account exists
//        Account account = accountRepository.findByAccountNumber(accountNumber)
//                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
//
//        // 2. Use BranchAuthorizationService to verify access
//        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
//            throw new UnauthorizedAccessException("Access denied: You do not have permission to view this account's balance");
//        }
//
//        // 3. Build and return DTO
//        AccountBalanceDTO dto = new AccountBalanceDTO();
//        dto.setAccountNumber(account.getAccountNumber());
//        dto.setCustomerId(account.getCustomer().getCustomerId());
//        dto.setAccountType(account.getAccountType());
//
//        // Add branch information
//        if (account.getBranch() != null) {
//            dto.setBranchCode(account.getBranch().getBranchCode());
//            dto.setBranchName(account.getBranch().getBranchName());
//        }
//
//        dto.setBalance(account.getBalance());
//        dto.setCurrency(account.getCurrency());
//        dto.setStatus(account.getStatus().name().toLowerCase());
//
//        log.info("Account balance retrieved successfully for {}", accountNumber);
//        return dto;
//    }
//
//    // ============================================
//    // HELPER METHODS
//    // ============================================
//
//    private BigDecimal calculateTransferFee(TransferRequestDTO request) {
//        // High priority transfers cost more
//        if ("high".equalsIgnoreCase(request.getPriority())) {
//            return HIGH_PRIORITY_FEE;
//        }
//
//        // Own account transfers are free
//        if ("own".equalsIgnoreCase(request.getTransferType())) {
//            return BigDecimal.ZERO;
//        }
//
//        // Standard NEFT fee
//        return NEFT_FEE;
//    }
//
//    private Transaction.TransferType determineTransferType(String type) {
//        if (type == null) return Transaction.TransferType.OTHER;
//        return "own".equalsIgnoreCase(type) ? Transaction.TransferType.OWN : Transaction.TransferType.OTHER;
//    }
//
//    private String generateTransactionId() {
//        return "TXN" + System.currentTimeMillis();
//    }
//
//    private String generateReferenceNumber() {
//        return "REF" + System.currentTimeMillis();
//    }
//
//    private String generateReceiptNumber() {
//        return "RCP" + System.currentTimeMillis();
//    }
//
//    private TransactionResponseDTO mapToResponseDTO(Transaction transaction) {
//        TransactionResponseDTO dto = new TransactionResponseDTO();
//        dto.setTransactionId(transaction.getTransactionId());
//        dto.setReferenceNumber(transaction.getReferenceNumber());
//        dto.setFromAccountNumber(transaction.getFromAccountNumber());
//        dto.setToAccountNumber(transaction.getToAccountNumber());
//
//        // Add branch information for from account
//        if (transaction.getFromAccount() != null && transaction.getFromAccount().getBranch() != null) {
//            dto.setFromBranchCode(transaction.getFromAccount().getBranch().getBranchCode());
//            dto.setFromBranchName(transaction.getFromAccount().getBranch().getBranchName());
//        }
//
//        // Add branch information for to account
//        if (transaction.getToAccount() != null && transaction.getToAccount().getBranch() != null) {
//            dto.setToBranchCode(transaction.getToAccount().getBranch().getBranchCode());
//            dto.setToBranchName(transaction.getToAccount().getBranch().getBranchName());
//        }
//
//        dto.setAmount(transaction.getAmount());
//        dto.setCurrency(transaction.getCurrency());
//        dto.setTransferFee(transaction.getTransferFee());
//        dto.setServiceTax(transaction.getServiceTax());
//        dto.setTotalAmount(transaction.getTotalAmount());
//        dto.setTransferMode(transaction.getTransferMode().name());
//        dto.setTransactionType(transaction.getTransactionType().name());
//        dto.setStatus(transaction.getStatus().name());
//        dto.setDescription(transaction.getDescription());
//        dto.setRemarks(transaction.getRemarks());
//        dto.setTimestamp(transaction.getTimestamp().toString());
//        dto.setCompletedAt(transaction.getCompletedAt() != null ? transaction.getCompletedAt().toString() : null);
//        dto.setReceiptNumber(transaction.getReceiptNumber());
//        dto.setBalanceBefore(transaction.getBalanceBefore());
//        dto.setBalanceAfter(transaction.getBalanceAfter());
//        dto.setBeneficiaryName(transaction.getBeneficiaryName());
//        dto.setBeneficiaryBank(transaction.getBeneficiaryBank());
//        dto.setFraudCheckPassed(transaction.getFraudCheckPassed());
//        dto.setRequiresApproval(transaction.getRequiresApproval());
//
//        return dto;
//    }
//}
//
//
////package com.izak.demoBankManagement.service;
////
////import com.izak.demoBankManagement.dto.*;
////import com.izak.demoBankManagement.entity.Account;
////import com.izak.demoBankManagement.entity.Transaction;
////import com.izak.demoBankManagement.exception.*;
////import com.izak.demoBankManagement.repository.AccountRepository;
////import com.izak.demoBankManagement.repository.TransactionRepository;
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.stereotype.Service;
////import org.springframework.transaction.annotation.Transactional;
////
////import java.math.BigDecimal;
////import java.time.LocalDateTime;
////import java.time.format.DateTimeFormatter;
////import java.util.UUID;
////import java.math.RoundingMode;
////
////@Service
////@RequiredArgsConstructor
////@Slf4j
////public class TransactionService {
////    private final AccountRepository accountRepository;
////    private final TransactionRepository transactionRepository;
////
////    private static final BigDecimal NEFT_FEE = new BigDecimal("2.00");
////    private static final BigDecimal HIGH_PRIORITY_FEE = new BigDecimal("7.00");
////    private static final BigDecimal SERVICE_TAX_RATE = new BigDecimal("0.18"); // 18%
////    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000.00");
////
////    // ============================================
////    // FUND TRANSFER (Account to Account)
////    // ============================================
////
////    @Transactional
////    public TransactionResponseDTO transferFunds(TransferRequestDTO request) {
////        log.info("Processing fund transfer from {} to {}", request.getFromAccountNumber(), request.getToAccountNumber());
////
////        // 1. Validate accounts exist and are active
////        Account fromAccount = accountRepository.findByAccountNumberWithLock(request.getFromAccountNumber())
////                .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + request.getFromAccountNumber()));
////
////        Account toAccount = accountRepository.findByAccountNumberWithLock(request.getToAccountNumber())
////                .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + request.getToAccountNumber()));
////
////        if (fromAccount.getStatus() != Account.Status.ACTIVE) {
////            throw new AccountInactiveException("Source account is not active");
////        }
////
////        if (toAccount.getStatus() != Account.Status.ACTIVE) {
////            throw new AccountInactiveException("Destination account is not active");
////        }
////
////        // 2. Calculate fees and total amount
////        BigDecimal transferFee = calculateTransferFee(request);
////        BigDecimal serviceTax = transferFee.multiply(SERVICE_TAX_RATE).setScale(2, BigDecimal.ROUND_HALF_UP);
////        BigDecimal totalAmount = request.getAmount().add(transferFee).add(serviceTax);
////
////        // 3. Check sufficient balance
////        if (fromAccount.getBalance().compareTo(totalAmount) < 0) {
////            throw new InsufficientBalanceException("Insufficient balance. Available: " + fromAccount.getBalance() + ", Required: " + totalAmount);
////        }
////
////        // 4. Perform transfer (debit from source, credit to destination)
////        BigDecimal fromBalanceBefore = fromAccount.getBalance();
////
////        fromAccount.setBalance(fromAccount.getBalance().subtract(totalAmount));
////        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
////
////        accountRepository.save(fromAccount);
////        accountRepository.save(toAccount);
////
////        // 5. Create transaction record
////        Transaction transaction = new Transaction();
////        transaction.setTransactionId(generateTransactionId());
////        transaction.setReferenceNumber(generateReferenceNumber());
////        transaction.setFromAccount(fromAccount);
////        transaction.setFromAccountNumber(fromAccount.getAccountNumber());
////        transaction.setToAccount(toAccount);
////        transaction.setToAccountNumber(toAccount.getAccountNumber());
////        transaction.setAmount(request.getAmount());
////        transaction.setCurrency("USD");
////        transaction.setTransferFee(transferFee);
////        transaction.setServiceTax(serviceTax);
////        transaction.setTotalAmount(totalAmount);
////        transaction.setTransferMode(Transaction.TransferMode.valueOf(request.getTransferMode().toUpperCase()));
////        transaction.setTransactionType(Transaction.TransactionType.TRANSFER);
////        transaction.setTransferType(determineTransferType(request.getTransferType()));
////        transaction.setStatus(Transaction.Status.COMPLETED);
////        transaction.setDescription(request.getDescription());
////        transaction.setRemarks(request.getRemarks());
////        transaction.setBalanceBefore(fromBalanceBefore);
////        transaction.setBalanceAfter(fromAccount.getBalance());
////        transaction.setTimestamp(LocalDateTime.now());
////        transaction.setCompletedAt(LocalDateTime.now());
////        transaction.setReceiptNumber(generateReceiptNumber());
////        transaction.setVerified(true);
////        transaction.setFraudCheckPassed(true);
////        transaction.setRequiresApproval(request.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0);
////        transaction.setBeneficiaryName(toAccount.getCustomer().getFirstName() + " " + toAccount.getCustomer().getLastName());
////        transaction.setBeneficiaryBank("Same Bank");
////        // REMOVED: transaction.setBeneficiaryBranch() - will get from toAccount.getBranch()
////
////        transactionRepository.save(transaction);
////
////        log.info("Fund transfer completed successfully. Transaction ID: {}", transaction.getTransactionId());
////
////        // 6. Return response DTO
////        return mapToResponseDTO(transaction);
////    }
//////    @Transactional
//////    public TransactionResponseDTO transferFunds(TransferRequestDTO request) {
//////        log.info("Processing fund transfer from {} to {}", request.getFromAccountNumber(), request.getToAccountNumber());
//////
//////        // 1. Validate accounts exist and are active
//////        Account fromAccount = accountRepository.findByAccountNumberWithLock(request.getFromAccountNumber())
//////                .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + request.getFromAccountNumber()));
//////
//////        Account toAccount = accountRepository.findByAccountNumberWithLock(request.getToAccountNumber())
//////                .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + request.getToAccountNumber()));
//////
//////        if (fromAccount.getStatus() != Account.Status.ACTIVE) {
//////            throw new AccountInactiveException("Source account is not active");
//////        }
//////
//////        if (toAccount.getStatus() != Account.Status.ACTIVE) {
//////            throw new AccountInactiveException("Destination account is not active");
//////        }
//////
//////        // 2. Calculate fees and total amount
//////        BigDecimal transferFee = calculateTransferFee(request);
//////        BigDecimal serviceTax = transferFee.multiply(SERVICE_TAX_RATE).setScale(2, BigDecimal.ROUND_HALF_UP);
//////        BigDecimal totalAmount = request.getAmount().add(transferFee).add(serviceTax);
//////
//////        // 3. Check sufficient balance
//////        if (fromAccount.getBalance().compareTo(totalAmount) < 0) {
//////            throw new InsufficientBalanceException("Insufficient balance. Available: " + fromAccount.getBalance() + ", Required: " + totalAmount);
//////        }
//////
//////        // 4. Perform transfer (debit from source, credit to destination)
//////        BigDecimal fromBalanceBefore = fromAccount.getBalance();
//////        BigDecimal toBalanceBefore = toAccount.getBalance();
//////
//////        fromAccount.setBalance(fromAccount.getBalance().subtract(totalAmount));
//////        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
//////
//////        accountRepository.save(fromAccount);
//////        accountRepository.save(toAccount);
//////
//////        // 5. Create transaction record
//////        Transaction transaction = new Transaction();
//////        transaction.setTransactionId(generateTransactionId());
//////        transaction.setReferenceNumber(generateReferenceNumber());
//////        transaction.setFromAccount(fromAccount);
//////        transaction.setFromAccountNumber(fromAccount.getAccountNumber());
//////        transaction.setToAccount(toAccount);
//////        transaction.setToAccountNumber(toAccount.getAccountNumber());
//////        transaction.setAmount(request.getAmount());
//////        transaction.setCurrency("USD");
//////        transaction.setTransferFee(transferFee);
//////        transaction.setServiceTax(serviceTax);
//////        transaction.setTotalAmount(totalAmount);
//////        transaction.setTransferMode(Transaction.TransferMode.valueOf(request.getTransferMode().toUpperCase()));
//////        transaction.setTransactionType(Transaction.TransactionType.TRANSFER);
//////        transaction.setTransferType(determineTransferType(request.getTransferType()));
//////        transaction.setStatus(Transaction.Status.COMPLETED);
//////        transaction.setDescription(request.getDescription());
//////        transaction.setRemarks(request.getRemarks());
//////        transaction.setBalanceBefore(fromBalanceBefore);
//////        transaction.setBalanceAfter(fromAccount.getBalance());
//////        transaction.setTimestamp(LocalDateTime.now());
//////        transaction.setCompletedAt(LocalDateTime.now());
//////        transaction.setReceiptNumber(generateReceiptNumber());
//////        transaction.setVerified(true);
//////        transaction.setFraudCheckPassed(true);
//////        transaction.setRequiresApproval(request.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0);
//////        transaction.setBeneficiaryName(toAccount.getCustomer().getFirstName() + " " + toAccount.getCustomer().getLastName());
//////        transaction.setBeneficiaryBank("Same Bank");
//////        transaction.setBeneficiaryBranch(toAccount.getBranch());
//////
//////        transactionRepository.save(transaction);
//////
//////        log.info("Fund transfer completed successfully. Transaction ID: {}", transaction.getTransactionId());
//////
//////        // 6. Return response DTO
//////        return mapToResponseDTO(transaction);
//////    }
////
////    // ============================================
////    // DEPOSIT MONEY
////    // ============================================
////    @Transactional
////    public TransactionResponseDTO depositMoney(DepositRequestDTO request) {
////        log.info("Processing deposit to account {}", request.getAccountNumber());
////
////        // 1. Validate account
////        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
////                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));
////
////        if (account.getStatus() != Account.Status.ACTIVE) {
////            throw new AccountInactiveException("Account is not active");
////        }
////
////        // 2. Update balance (no fees for deposits)
////        BigDecimal balanceBefore = account.getBalance();
////        account.setBalance(account.getBalance().add(request.getAmount()));
////        accountRepository.save(account);
////
////        // 3. Create transaction record
////        Transaction transaction = new Transaction();
////        transaction.setTransactionId(generateTransactionId());
////        transaction.setReferenceNumber(generateReferenceNumber());
////        transaction.setFromAccount(null); // No source account for deposits
////        transaction.setFromAccountNumber("CASH_DEPOSIT");
////        transaction.setToAccount(account);
////        transaction.setToAccountNumber(account.getAccountNumber());
////        transaction.setAmount(request.getAmount());
////        transaction.setCurrency("USD");
////        transaction.setTransferFee(BigDecimal.ZERO);
////        transaction.setServiceTax(BigDecimal.ZERO);
////        transaction.setTotalAmount(request.getAmount());
////        transaction.setTransferMode(Transaction.TransferMode.valueOf(request.getDepositMode().toUpperCase()));
////        transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
////        transaction.setStatus(Transaction.Status.COMPLETED);
////        transaction.setDescription(request.getDescription());
////        transaction.setRemarks(request.getRemarks());
////        transaction.setBalanceBefore(balanceBefore);
////        transaction.setBalanceAfter(account.getBalance());
////        transaction.setTimestamp(LocalDateTime.now());
////        transaction.setCompletedAt(LocalDateTime.now());
////        transaction.setReceiptNumber(generateReceiptNumber());
////        transaction.setVerified(true);
////        transaction.setFraudCheckPassed(true);
////        transaction.setRequiresApproval(false);
////
////        transactionRepository.save(transaction);
////
////        log.info("Deposit completed successfully. Transaction ID: {}", transaction.getTransactionId());
////
////        return mapToResponseDTO(transaction);
////    }
////
////    // ============================================
////    // WITHDRAW MONEY
////    // ============================================
////    @Transactional
////    public TransactionResponseDTO withdrawMoney(WithdrawRequestDTO request) {
////        log.info("Processing withdrawal from account {}", request.getAccountNumber());
////
////        // 1. Validate account
////        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
////                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));
////
////        if (account.getStatus() != Account.Status.ACTIVE) {
////            throw new AccountInactiveException("Account is not active");
////        }
////
////        // 2. Check sufficient balance (no fees for withdrawals)
////        if (account.getBalance().compareTo(request.getAmount()) < 0) {
////            throw new InsufficientBalanceException("Insufficient balance. Available: " + account.getBalance() + ", Requested: " + request.getAmount());
////        }
////
////        // 3. Update balance
////        BigDecimal balanceBefore = account.getBalance();
////        account.setBalance(account.getBalance().subtract(request.getAmount()));
////        accountRepository.save(account);
////
////        // 4. Create transaction record
////        Transaction transaction = new Transaction();
////        transaction.setTransactionId(generateTransactionId());
////        transaction.setReferenceNumber(generateReferenceNumber());
////        transaction.setFromAccount(account);
////        transaction.setFromAccountNumber(account.getAccountNumber());
////        transaction.setToAccount(null); // No destination account for withdrawals
////        transaction.setToAccountNumber("CASH_WITHDRAWAL");
////        transaction.setAmount(request.getAmount());
////        transaction.setCurrency("USD");
////        transaction.setTransferFee(BigDecimal.ZERO);
////        transaction.setServiceTax(BigDecimal.ZERO);
////        transaction.setTotalAmount(request.getAmount());
////        transaction.setTransferMode(Transaction.TransferMode.valueOf(request.getWithdrawalMode().toUpperCase()));
////        transaction.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
////        transaction.setStatus(Transaction.Status.COMPLETED);
////        transaction.setDescription(request.getDescription());
////        transaction.setRemarks(request.getRemarks());
////        transaction.setBalanceBefore(balanceBefore);
////        transaction.setBalanceAfter(account.getBalance());
////        transaction.setTimestamp(LocalDateTime.now());
////        transaction.setCompletedAt(LocalDateTime.now());
////        transaction.setReceiptNumber(generateReceiptNumber());
////        transaction.setVerified(true);
////        transaction.setFraudCheckPassed(true);
////        transaction.setRequiresApproval(false);
////
////        transactionRepository.save(transaction);
////
////        log.info("Withdrawal completed successfully. Transaction ID: {}", transaction.getTransactionId());
////
////        return mapToResponseDTO(transaction);
////    }
////
////    // ============================================
////    // GET ACCOUNT BALANCE
////    // ============================================
////
////    public AccountBalanceDTO getAccountBalance(String accountNumber) {
////        Account account = accountRepository.findByAccountNumber(accountNumber)
////                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
////
////        AccountBalanceDTO dto = new AccountBalanceDTO();
////        dto.setAccountNumber(account.getAccountNumber());
////        dto.setCustomerId(account.getCustomer().getCustomerId());
////        dto.setAccountType(account.getAccountType());
////
////        // Add branch information
////        if (account.getBranch() != null) {
////            dto.setBranchCode(account.getBranch().getBranchCode());
////            dto.setBranchName(account.getBranch().getBranchName());
////        }
////
////        dto.setBalance(account.getBalance());
////        dto.setCurrency(account.getCurrency());
////        dto.setStatus(account.getStatus().name().toLowerCase());
////
////        return dto;
////    }
////
//////    public AccountBalanceDTO getAccountBalance(String accountNumber) {
//////        Account account = accountRepository.findByAccountNumber(accountNumber)
//////                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
//////
//////        AccountBalanceDTO dto = new AccountBalanceDTO();
//////        dto.setAccountNumber(account.getAccountNumber());
//////        dto.setCustomerId(account.getCustomer().getCustomerId());
//////        dto.setAccountType(account.getAccountType());
//////        dto.setBalance(account.getBalance());
//////        dto.setCurrency(account.getCurrency());
//////        dto.setStatus(account.getStatus().name().toLowerCase());
//////
//////        return dto;
//////    }
////
////    // ============================================
////    // HELPER METHODS
////    // ============================================
////
////    private BigDecimal calculateTransferFee(TransferRequestDTO request) {
////        // High priority transfers cost more
////        if ("high".equalsIgnoreCase(request.getPriority())) {
////            return HIGH_PRIORITY_FEE;
////        }
////
////        // Own account transfers are free
////        if ("own".equalsIgnoreCase(request.getTransferType())) {
////            return BigDecimal.ZERO;
////        }
////
////        // Standard NEFT fee
////        return NEFT_FEE;
////    }
////
////    private Transaction.TransferType determineTransferType(String type) {
////        if (type == null) return Transaction.TransferType.OTHER;
////        return "own".equalsIgnoreCase(type) ? Transaction.TransferType.OWN : Transaction.TransferType.OTHER;
////    }
////
////    private String generateTransactionId() {
////        return "TXN" + System.currentTimeMillis();
////    }
////
////    private String generateReferenceNumber() {
////        return "REF" + System.currentTimeMillis();
////    }
////
////    private String generateReceiptNumber() {
////        return "RCP" + System.currentTimeMillis();
////    }
////
////    private TransactionResponseDTO mapToResponseDTO(Transaction transaction) {
////        TransactionResponseDTO dto = new TransactionResponseDTO();
////        dto.setTransactionId(transaction.getTransactionId());
////        dto.setReferenceNumber(transaction.getReferenceNumber());
////        dto.setFromAccountNumber(transaction.getFromAccountNumber());
////        dto.setToAccountNumber(transaction.getToAccountNumber());
////
////        // Add branch information for from account
////        if (transaction.getFromAccount() != null && transaction.getFromAccount().getBranch() != null) {
////            dto.setFromBranchCode(transaction.getFromAccount().getBranch().getBranchCode());
////            dto.setFromBranchName(transaction.getFromAccount().getBranch().getBranchName());
////        }
////
////        // Add branch information for to account
////        if (transaction.getToAccount() != null && transaction.getToAccount().getBranch() != null) {
////            dto.setToBranchCode(transaction.getToAccount().getBranch().getBranchCode());
////            dto.setToBranchName(transaction.getToAccount().getBranch().getBranchName());
////        }
////
////        dto.setAmount(transaction.getAmount());
////        dto.setCurrency(transaction.getCurrency());
////        dto.setTransferFee(transaction.getTransferFee());
////        dto.setServiceTax(transaction.getServiceTax());
////        dto.setTotalAmount(transaction.getTotalAmount());
////        dto.setTransferMode(transaction.getTransferMode().name());
////        dto.setTransactionType(transaction.getTransactionType().name());
////        dto.setStatus(transaction.getStatus().name());
////        dto.setDescription(transaction.getDescription());
////        dto.setRemarks(transaction.getRemarks());
////        dto.setTimestamp(transaction.getTimestamp().toString());
////        dto.setCompletedAt(transaction.getCompletedAt() != null ? transaction.getCompletedAt().toString() : null);
////        dto.setReceiptNumber(transaction.getReceiptNumber());
////        dto.setBalanceBefore(transaction.getBalanceBefore());
////        dto.setBalanceAfter(transaction.getBalanceAfter());
////        dto.setBeneficiaryName(transaction.getBeneficiaryName());
////        dto.setBeneficiaryBank(transaction.getBeneficiaryBank());
////        dto.setFraudCheckPassed(transaction.getFraudCheckPassed());
////        dto.setRequiresApproval(transaction.getRequiresApproval());
////
////        return dto;
////    }
////
////
//////    private TransactionResponseDTO mapToResponseDTO(Transaction transaction) {
//////        TransactionResponseDTO dto = new TransactionResponseDTO();
//////        dto.setTransactionId(transaction.getTransactionId());
//////        dto.setReferenceNumber(transaction.getReferenceNumber());
//////        dto.setFromAccountNumber(transaction.getFromAccountNumber());
//////        dto.setToAccountNumber(transaction.getToAccountNumber());
//////        dto.setAmount(transaction.getAmount());
//////        dto.setCurrency(transaction.getCurrency());
//////        dto.setTransferFee(transaction.getTransferFee());
//////        dto.setServiceTax(transaction.getServiceTax());
//////        dto.setTotalAmount(transaction.getTotalAmount());
//////        dto.setTransferMode(transaction.getTransferMode().name());
//////        dto.setTransactionType(transaction.getTransactionType().name());
//////        dto.setStatus(transaction.getStatus().name());
//////        dto.setDescription(transaction.getDescription());
//////        dto.setRemarks(transaction.getRemarks());
//////        dto.setTimestamp(transaction.getTimestamp().toString());
//////        dto.setCompletedAt(transaction.getCompletedAt() != null ? transaction.getCompletedAt().toString() : null);
//////        dto.setReceiptNumber(transaction.getReceiptNumber());
//////        dto.setBalanceBefore(transaction.getBalanceBefore());
//////        dto.setBalanceAfter(transaction.getBalanceAfter());
//////        dto.setBeneficiaryName(transaction.getBeneficiaryName());
//////        dto.setBeneficiaryBank(transaction.getBeneficiaryBank());
//////        dto.setFraudCheckPassed(transaction.getFraudCheckPassed());
//////        dto.setRequiresApproval(transaction.getRequiresApproval());
//////
//////        return dto;
//////    }
////}

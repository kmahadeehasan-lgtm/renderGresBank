package com.izak.demoBankManagement.service;
import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.entity.Account;
import com.izak.demoBankManagement.entity.Branch;
import com.izak.demoBankManagement.entity.Customer;
import com.izak.demoBankManagement.entity.Transaction;
import com.izak.demoBankManagement.exception.*;
import com.izak.demoBankManagement.repository.AccountRepository;
import com.izak.demoBankManagement.repository.BranchRepository;
import com.izak.demoBankManagement.repository.CustomerRepository;
import com.izak.demoBankManagement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final BranchRepository branchRepository;


    private final BranchAuthorizationService branchAuthorizationService;

    // ============================================
    // CREATE ACCOUNT
    // ============================================
//    @Transactional
//    public AccountResponseDTO createAccount(AccountCreateRequestDTO request) {
//        log.info("Creating new account for customer: {}", request.getCustomerId());
//
//        // 1. Validate customer exists
//        Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
//                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + request.getCustomerId()));
//
//        if (customer.getStatus() != Customer.Status.ACTIVE) {
//            throw new InvalidTransactionException("Cannot create account for inactive customer");
//        }
//
//        // 2. Create Account
//        Account account = new Account();
//        account.setAccountNumber(generateAccountNumber());
//        account.setCustomer(customer);
//        account.setCustomerId(customer.getCustomerId());
//        account.setAccountType(request.getAccountType());
//        account.setBranch(request.getBranch());
//        account.setBalance(request.getBalance());
//        account.setCurrency(request.getCurrency());
//        account.setInterestRate(request.getInterestRate());
//        account.setNomineeFirstName(request.getNomineeFirstName());
//        account.setNomineeLastName(request.getNomineeLastName());
//        account.setNomineeRelationship(request.getNomineeRelationship());
//        account.setNomineePhone(request.getNomineePhone());
//        account.setStatus(Account.Status.ACTIVE);
//        account.setKycStatus(customer.getKycStatus() == Customer.KycStatus.VERIFIED ?
//                Account.KycStatus.VERIFIED : Account.KycStatus.PENDING);
//
//        account = accountRepository.save(account);
//
//        log.info("Account created successfully: {}", account.getAccountNumber());
//
//        return mapToResponseDTO(account);
//    }


    @Transactional
    public AccountResponseDTO createAccount(AccountCreateRequestDTO request) {
        log.info("Creating new account for customer: {}", request.getCustomerId());

        // 1. Validate customer exists
        Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + request.getCustomerId()));

        if (customer.getStatus() != Customer.Status.ACTIVE) {
            throw new InvalidTransactionException("Cannot create account for inactive customer");
        }

        // 2. Validate and fetch branch by branch code
        Branch branch = branchRepository.findByBranchCode(request.getBranchCode())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchCode()));

        if (branch.getStatus() != Branch.BranchStatus.ACTIVE) {
            throw new InvalidTransactionException("Cannot create account for inactive branch");
        }

        // 3. Create Account
        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setCustomer(customer);
        account.setCustomerId(customer.getCustomerId());
        account.setAccountType(request.getAccountType());
        account.setBranch(branch); // Set Branch entity
        account.setBalance(request.getBalance());
        account.setCurrency(request.getCurrency());
        account.setInterestRate(request.getInterestRate());
        account.setNomineeFirstName(request.getNomineeFirstName());
        account.setNomineeLastName(request.getNomineeLastName());
        account.setNomineeRelationship(request.getNomineeRelationship());
        account.setNomineePhone(request.getNomineePhone());
        account.setStatus(Account.Status.ACTIVE);
        account.setKycStatus(customer.getKycStatus() == Customer.KycStatus.VERIFIED ?
                Account.KycStatus.VERIFIED : Account.KycStatus.PENDING);

        account = accountRepository.save(account);

        log.info("Account created successfully: {}", account.getAccountNumber());

        return mapToResponseDTO(account);
    }



    // ============================================
    // GET ACCOUNT BY ID
    // ============================================
//    public AccountResponseDTO getAccountById(Long id) {
//        Account account = accountRepository.findById(id)
//                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));
//
//        return mapToResponseDTO(account);
//    }

    // ============================================
    // GET ACCOUNT BY ACCOUNT NUMBER
    // ============================================
//    public AccountResponseDTO getAccountByAccountNumber(String accountNumber) {
//        Account account = accountRepository.findByAccountNumber(accountNumber)
//                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
//
//        return mapToResponseDTO(account);
//    }

    // ============================================
    // GET ALL ACCOUNTS
    // ============================================
//    public List<AccountListItemDTO> getAllAccounts() {
//        return accountRepository.findAll().stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }



    // ============================================
    // GET ACCOUNTS BY CUSTOMER ID
    // ============================================
//    public List<AccountListItemDTO> getAccountsByCustomerId(String customerId) {
//        return accountRepository.findByCustomerId(customerId).stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }

    // ============================================
    // GET ACCOUNTS BY STATUS
    // ============================================
    public List<AccountListItemDTO> getAccountsByStatus(String status) {
        Account.Status accountStatus = Account.Status.valueOf(status.toUpperCase());
        return accountRepository.findByStatus(accountStatus).stream()
                .map(this::mapToListItemDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // UPDATE ACCOUNT
    // ============================================

//    @Transactional
//    public AccountResponseDTO updateAccount(Long id, AccountUpdateRequestDTO request) {
//        log.info("Updating account with ID: {}", id);
//
//        Account account = accountRepository.findById(id)
//                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));
//
//        // Update branch if provided
//        if (request.getBranchCode() != null) {
//            Branch branch = branchRepository.findByBranchCode(request.getBranchCode())
//                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchCode()));
//
//            if (branch.getStatus() != Branch.BranchStatus.ACTIVE) {
//                throw new InvalidTransactionException("Cannot transfer account to inactive branch");
//            }
//
//            account.setBranch(branch);
//            log.info("Account {} transferred to branch {}", account.getAccountNumber(), branch.getBranchName());
//        }
//
//        if (request.getInterestRate() != null) {
//            account.setInterestRate(request.getInterestRate());
//        }
//        if (request.getNomineeFirstName() != null) {
//            account.setNomineeFirstName(request.getNomineeFirstName());
//        }
//        if (request.getNomineeLastName() != null) {
//            account.setNomineeLastName(request.getNomineeLastName());
//        }
//        if (request.getNomineeRelationship() != null) {
//            account.setNomineeRelationship(request.getNomineeRelationship());
//        }
//        if (request.getNomineePhone() != null) {
//            account.setNomineePhone(request.getNomineePhone());
//        }
//        if (request.getStatus() != null) {
//            account.setStatus(Account.Status.valueOf(request.getStatus().toUpperCase()));
//        }
//        if (request.getKycStatus() != null) {
//            account.setKycStatus(Account.KycStatus.valueOf(request.getKycStatus().toUpperCase()));
//        }
//
//        account = accountRepository.save(account);
//
//        log.info("Account updated successfully: {}", account.getAccountNumber());
//
//        return mapToResponseDTO(account);
//    }




//    @Transactional
//    public AccountResponseDTO updateAccount(Long id, AccountUpdateRequestDTO request) {
//        log.info("Updating account with ID: {}", id);
//
//        Account account = accountRepository.findById(id)
//                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));
//
//        // Update fields if provided
//        if (request.getBranch() != null) {
//            account.setBranch(request.getBranch());
//        }
//        if (request.getInterestRate() != null) {
//            account.setInterestRate(request.getInterestRate());
//        }
//        if (request.getNomineeFirstName() != null) {
//            account.setNomineeFirstName(request.getNomineeFirstName());
//        }
//        if (request.getNomineeLastName() != null) {
//            account.setNomineeLastName(request.getNomineeLastName());
//        }
//        if (request.getNomineeRelationship() != null) {
//            account.setNomineeRelationship(request.getNomineeRelationship());
//        }
//        if (request.getNomineePhone() != null) {
//            account.setNomineePhone(request.getNomineePhone());
//        }
//        if (request.getStatus() != null) {
//            account.setStatus(Account.Status.valueOf(request.getStatus().toUpperCase()));
//        }
//        if (request.getKycStatus() != null) {
//            account.setKycStatus(Account.KycStatus.valueOf(request.getKycStatus().toUpperCase()));
//        }
//
//        account = accountRepository.save(account);
//
//        log.info("Account updated successfully: {}", account.getAccountNumber());
//
//        return mapToResponseDTO(account);
//    }

    // ============================================
    // DELETE ACCOUNT (Soft Delete - Set Status to CLOSED)
    // ============================================
    @Transactional
//    public void deleteAccount(Long id) {
//        log.info("Closing account with ID: {}", id);
//
//        Account account = accountRepository.findById(id)
//                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));
//
//        // Check if account has balance
//        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
//            throw new InvalidTransactionException("Cannot close account with non-zero balance. Current balance: " + account.getBalance());
//        }
//
//        // Soft delete - set status to closed
//        account.setStatus(Account.Status.CLOSED);
//        accountRepository.save(account);
//
//        log.info("Account closed: {}", account.getAccountNumber());
//    }

    // ============================================
    // FREEZE ACCOUNT
    // ============================================
//    @Transactional
//    public AccountResponseDTO freezeAccount(String accountNumber) {
//        log.info("Freezing account: {}", accountNumber);
//
//        Account account = accountRepository.findByAccountNumber(accountNumber)
//                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
//
//        account.setStatus(Account.Status.FROZEN);
//        account = accountRepository.save(account);
//
//        log.info("Account frozen successfully");
//
//        return mapToResponseDTO(account);
//    }

    // ============================================
    // UNFREEZE ACCOUNT
    // ============================================
//    @Transactional
//    public AccountResponseDTO unfreezeAccount(String accountNumber) {
//        log.info("Unfreezing account: {}", accountNumber);
//
//        Account account = accountRepository.findByAccountNumber(accountNumber)
//                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
//
//        if (account.getStatus() != Account.Status.FROZEN) {
//            throw new InvalidTransactionException("Account is not frozen");
//        }
//
//        account.setStatus(Account.Status.ACTIVE);
//        account = accountRepository.save(account);
//
//        log.info("Account unfrozen successfully");
//
//        return mapToResponseDTO(account);
//    }

    // ============================================
    // GET ACCOUNT STATEMENT
    // ============================================

//    public AccountStatementDTO getAccountStatement(AccountStatementRequestDTO request) {
//        log.info("Generating statement for account: {} from {} to {}",
//                request.getAccountNumber(), request.getStartDate(), request.getEndDate());
//
//        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
//                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));
//
//        // Get transactions within date range
//        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(
//                account.getId(),
//                request.getStartDate(),
//                request.getEndDate()
//        );
//
//        // Calculate opening balance
//        BigDecimal openingBalance = account.getBalance();
//        if (!transactions.isEmpty()) {
//            for (Transaction txn : transactions) {
//                if (txn.getFromAccount() != null && txn.getFromAccount().getId().equals(account.getId())) {
//                    openingBalance = openingBalance.add(txn.getTotalAmount());
//                } else if (txn.getToAccount() != null && txn.getToAccount().getId().equals(account.getId())) {
//                    openingBalance = openingBalance.subtract(txn.getAmount());
//                }
//            }
//        }
//
//        // Calculate totals
//        BigDecimal totalCredits = BigDecimal.ZERO;
//        BigDecimal totalDebits = BigDecimal.ZERO;
//
//        for (Transaction txn : transactions) {
//            if (txn.getToAccount() != null && txn.getToAccount().getId().equals(account.getId())) {
//                totalCredits = totalCredits.add(txn.getAmount());
//            }
//            if (txn.getFromAccount() != null && txn.getFromAccount().getId().equals(account.getId())) {
//                totalDebits = totalDebits.add(txn.getTotalAmount());
//            }
//        }
//
//        // Map transactions to DTO
//        List<TransactionHistoryDTO> transactionDTOs = transactions.stream()
//                .map(txn -> mapToTransactionHistoryDTO(txn, account.getAccountNumber()))
//                .collect(Collectors.toList());
//
//        // Build statement DTO
//        AccountStatementDTO statement = new AccountStatementDTO();
//        statement.setAccountNumber(account.getAccountNumber());
//        statement.setAccountType(account.getAccountType());
//        statement.setCustomerName(account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName());
//        statement.setCustomerEmail(account.getCustomer().getEmail());
//
//        // Add branch information
//        if (account.getBranch() != null) {
//            statement.setBranchCode(account.getBranch().getBranchCode());
//            statement.setBranchName(account.getBranch().getBranchName());
//            statement.setBranchAddress(account.getBranch().getAddress() + ", " +
//                    account.getBranch().getCity() + ", " +
//                    account.getBranch().getState());
//        }
//
//        statement.setStatementStartDate(request.getStartDate());
//        statement.setStatementEndDate(request.getEndDate());
//        statement.setOpeningBalance(openingBalance);
//        statement.setClosingBalance(account.getBalance());
//        statement.setTotalCredits(totalCredits);
//        statement.setTotalDebits(totalDebits);
//        statement.setTransactionCount(transactions.size());
//        statement.setTransactions(transactionDTOs);
//
//        return statement;
//    }

//    public AccountStatementDTO getAccountStatement(AccountStatementRequestDTO request) {
//        log.info("Generating statement for account: {} from {} to {}",
//                request.getAccountNumber(), request.getStartDate(), request.getEndDate());
//
//        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
//                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));
//
//        // Get transactions within date range
//        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(
//                account.getId(),
//                request.getStartDate(),
//                request.getEndDate()
//        );
//
//        // Calculate opening balance (balance before first transaction)
//        BigDecimal openingBalance = account.getBalance();
//        if (!transactions.isEmpty()) {
//            // Work backwards from current balance
//            for (Transaction txn : transactions) {
//                if (txn.getFromAccount() != null && txn.getFromAccount().getId().equals(account.getId())) {
//                    openingBalance = openingBalance.add(txn.getTotalAmount());
//                } else if (txn.getToAccount() != null && txn.getToAccount().getId().equals(account.getId())) {
//                    openingBalance = openingBalance.subtract(txn.getAmount());
//                }
//            }
//        }
//
//        // Calculate totals
//        BigDecimal totalCredits = BigDecimal.ZERO;
//        BigDecimal totalDebits = BigDecimal.ZERO;
//
//        for (Transaction txn : transactions) {
//            if (txn.getToAccount() != null && txn.getToAccount().getId().equals(account.getId())) {
//                totalCredits = totalCredits.add(txn.getAmount());
//            }
//            if (txn.getFromAccount() != null && txn.getFromAccount().getId().equals(account.getId())) {
//                totalDebits = totalDebits.add(txn.getTotalAmount());
//            }
//        }
//
//        // Map transactions to DTO
//        List<TransactionHistoryDTO> transactionDTOs = transactions.stream()
//                .map(txn -> mapToTransactionHistoryDTO(txn, account.getAccountNumber()))
//                .collect(Collectors.toList());
//
//        // Build statement DTO
//        AccountStatementDTO statement = new AccountStatementDTO();
//        statement.setAccountNumber(account.getAccountNumber());
//        statement.setAccountType(account.getAccountType());
//        statement.setCustomerName(account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName());
//        statement.setCustomerEmail(account.getCustomer().getEmail());
//        statement.setBranch(account.getBranch());
//        statement.setStatementStartDate(request.getStartDate());
//        statement.setStatementEndDate(request.getEndDate());
//        statement.setOpeningBalance(openingBalance);
//        statement.setClosingBalance(account.getBalance());
//        statement.setTotalCredits(totalCredits);
//        statement.setTotalDebits(totalDebits);
//        statement.setTransactionCount(transactions.size());
//        statement.setTransactions(transactionDTOs);
//
//        return statement;
//    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private String generateAccountNumber() {
        String accountNumber;
        do {
            long randomNum = (long) (Math.random() * 10000000000L);
            accountNumber = "ACC0" + randomNum;
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }



    // =============================================
    //             Map To
    // =============================================

    private AccountResponseDTO mapToResponseDTO(Account account) {
        AccountResponseDTO dto = new AccountResponseDTO();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setCustomerId(account.getCustomerId());
        dto.setAccountType(account.getAccountType());

        // Map branch information
        if (account.getBranch() != null) {
            dto.setBranchId(account.getBranch().getId());
            dto.setBranchCode(account.getBranch().getBranchCode());
            dto.setBranchName(account.getBranch().getBranchName());
            dto.setBranchCity(account.getBranch().getCity());
        }

        dto.setBalance(account.getBalance());
        dto.setCurrency(account.getCurrency());
        dto.setInterestRate(account.getInterestRate());
        dto.setNomineeFirstName(account.getNomineeFirstName());
        dto.setNomineeLastName(account.getNomineeLastName());
        dto.setNomineeRelationship(account.getNomineeRelationship());
        dto.setNomineePhone(account.getNomineePhone());
        dto.setStatus(account.getStatus().name().toLowerCase());
        dto.setKycStatus(account.getKycStatus().name().toLowerCase());
        dto.setCreatedDate(account.getCreatedDate());
        dto.setLastUpdated(account.getLastUpdated());
        dto.setCustomerName(account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName());
        dto.setCustomerEmail(account.getCustomer().getEmail());

        // Calculate transaction summary
        int totalTransactions = 0;
        if (account.getOutgoingTransactions() != null) {
            totalTransactions += account.getOutgoingTransactions().size();
        }
        if (account.getIncomingTransactions() != null) {
            totalTransactions += account.getIncomingTransactions().size();
        }
        dto.setTotalTransactions(totalTransactions);

        return dto;
    }

    private AccountListItemDTO mapToListItemDTO(Account account) {
        AccountListItemDTO dto = new AccountListItemDTO();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setCustomerId(account.getCustomerId());
        dto.setCustomerName(account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName());
        dto.setAccountType(account.getAccountType());

        // Map branch information
        if (account.getBranch() != null) {
            dto.setBranchId(account.getBranch().getId());
            dto.setBranchCode(account.getBranch().getBranchCode());
            dto.setBranchName(account.getBranch().getBranchName());
        }

        dto.setBalance(account.getBalance());
        dto.setCurrency(account.getCurrency());
        dto.setStatus(account.getStatus().name().toLowerCase());
        dto.setKycStatus(account.getKycStatus().name().toLowerCase());
        dto.setCreatedDate(account.getCreatedDate());

        return dto;
    }

    private TransactionHistoryDTO mapToTransactionHistoryDTO(Transaction txn, String accountNumber) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        dto.setTransactionId(txn.getTransactionId());
        dto.setReferenceNumber(txn.getReferenceNumber());
        dto.setAccountNumber(accountNumber);

        // Determine if this is a debit or credit from the account's perspective
        boolean isDebit = txn.getFromAccountNumber().equals(accountNumber);
        dto.setTransactionType(isDebit ? "DEBIT" : "CREDIT");
        dto.setOtherAccountNumber(isDebit ? txn.getToAccountNumber() : txn.getFromAccountNumber());

        // Add branch information
        if (isDebit && txn.getFromAccount() != null && txn.getFromAccount().getBranch() != null) {
            dto.setBranchCode(txn.getFromAccount().getBranch().getBranchCode());
            dto.setBranchName(txn.getFromAccount().getBranch().getBranchName());
        }
        if (!isDebit && txn.getToAccount() != null && txn.getToAccount().getBranch() != null) {
            dto.setBranchCode(txn.getToAccount().getBranch().getBranchCode());
            dto.setBranchName(txn.getToAccount().getBranch().getBranchName());
        }

        // Other branch info
        if (isDebit && txn.getToAccount() != null && txn.getToAccount().getBranch() != null) {
            dto.setOtherBranchCode(txn.getToAccount().getBranch().getBranchCode());
            dto.setOtherBranchName(txn.getToAccount().getBranch().getBranchName());
        }
        if (!isDebit && txn.getFromAccount() != null && txn.getFromAccount().getBranch() != null) {
            dto.setOtherBranchCode(txn.getFromAccount().getBranch().getBranchCode());
            dto.setOtherBranchName(txn.getFromAccount().getBranch().getBranchName());
        }

        dto.setAmount(txn.getAmount());
        dto.setTransferMode(txn.getTransferMode().name());
        dto.setStatus(txn.getStatus().name().toLowerCase());
        dto.setDescription(txn.getDescription());
        dto.setTimestamp(txn.getTimestamp().toString());
        dto.setBalanceAfter(txn.getBalanceAfter());

        return dto;
    }



//    private AccountResponseDTO mapToResponseDTO(Account account) {
//        AccountResponseDTO dto = new AccountResponseDTO();
//        dto.setId(account.getId());
//        dto.setAccountNumber(account.getAccountNumber());
//        dto.setCustomerId(account.getCustomerId());
//        dto.setAccountType(account.getAccountType());
//        dto.setBranch(account.getBranch());
//        dto.setBalance(account.getBalance());
//        dto.setCurrency(account.getCurrency());
//        dto.setInterestRate(account.getInterestRate());
//        dto.setNomineeFirstName(account.getNomineeFirstName());
//        dto.setNomineeLastName(account.getNomineeLastName());
//        dto.setNomineeRelationship(account.getNomineeRelationship());
//        dto.setNomineePhone(account.getNomineePhone());
//        dto.setStatus(account.getStatus().name().toLowerCase());
//        dto.setKycStatus(account.getKycStatus().name().toLowerCase());
//        dto.setCreatedDate(account.getCreatedDate());
//        dto.setLastUpdated(account.getLastUpdated());
//        dto.setCustomerName(account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName());
//        dto.setCustomerEmail(account.getCustomer().getEmail());
//
//        // Calculate transaction summary
//        int totalTransactions = 0;
//        if (account.getOutgoingTransactions() != null) {
//            totalTransactions += account.getOutgoingTransactions().size();
//        }
//        if (account.getIncomingTransactions() != null) {
//            totalTransactions += account.getIncomingTransactions().size();
//        }
//        dto.setTotalTransactions(totalTransactions);
//
//        return dto;
//    }
//
//    private AccountListItemDTO mapToListItemDTO(Account account) {
//        AccountListItemDTO dto = new AccountListItemDTO();
//        dto.setId(account.getId());
//        dto.setAccountNumber(account.getAccountNumber());
//        dto.setCustomerId(account.getCustomerId());
//        dto.setCustomerName(account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName());
//        dto.setAccountType(account.getAccountType());
//        dto.setBranch(account.getBranch());
//        dto.setBalance(account.getBalance());
//        dto.setCurrency(account.getCurrency());
//        dto.setStatus(account.getStatus().name().toLowerCase());
//        dto.setKycStatus(account.getKycStatus().name().toLowerCase());
//        dto.setCreatedDate(account.getCreatedDate());
//
//        return dto;
//    }
//
//    private TransactionHistoryDTO mapToTransactionHistoryDTO(Transaction txn, String accountNumber) {
//        TransactionHistoryDTO dto = new TransactionHistoryDTO();
//        dto.setTransactionId(txn.getTransactionId());
//        dto.setReferenceNumber(txn.getReferenceNumber());
//        dto.setAccountNumber(accountNumber);
//
//        // Determine if this is a debit or credit from the account's perspective
//        boolean isDebit = txn.getFromAccountNumber().equals(accountNumber);
//        dto.setTransactionType(isDebit ? "DEBIT" : "CREDIT");
//        dto.setOtherAccountNumber(isDebit ? txn.getToAccountNumber() : txn.getFromAccountNumber());
//
//        dto.setAmount(txn.getAmount());
//        dto.setTransferMode(txn.getTransferMode().name());
//        dto.setStatus(txn.getStatus().name().toLowerCase());
//        dto.setDescription(txn.getDescription());
//        dto.setTimestamp(txn.getTimestamp().toString());
//        dto.setBalanceAfter(txn.getBalanceAfter());
//
//        return dto;
//    }












































    // ============================================
// GET ACCOUNT BY ID (with Authorization)
// ============================================
    public AccountResponseDTO getAccountById(Long id, String jwtToken) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));

        // Authorization check
        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
            log.warn("Unauthorized access attempt to account {} by user with role {}",
                    account.getAccountNumber(), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions for this account");
        }

        return mapToResponseDTO(account);
    }

    // ============================================
// GET ACCOUNT BY ACCOUNT NUMBER (with Authorization)
// ============================================
    public AccountResponseDTO getAccountByAccountNumber(String accountNumber, String jwtToken) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        // Authorization check
        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
            log.warn("Unauthorized access attempt to account {} by user with role {}",
                    accountNumber, branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions for this account");
        }

        return mapToResponseDTO(account);
    }

    // ============================================
// UPDATE ACCOUNT (with Authorization)
// ============================================
    @Transactional
    public AccountResponseDTO updateAccount(Long id, AccountUpdateRequestDTO request, String jwtToken) {
        log.info("Updating account with ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));

        // Authorization check
        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
            log.warn("Unauthorized update attempt on account {} by user with role {}",
                    account.getAccountNumber(), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions for this account");
        }

        // Extract role to check for BRANCH_MANAGER restrictions
        String role = branchAuthorizationService.extractRole(jwtToken);

        // BRANCH_MANAGER cannot change the branch
        if ("BRANCH_MANAGER".equals(role)) {
            if (request.getBranchCode() != null) {
                log.warn("Branch Manager attempted to change account branch - operation denied");
                request.setBranchCode(null); // Prevent branch change
            }
        }

        // Update branch if provided (and user is not BRANCH_MANAGER)
        if (request.getBranchCode() != null) {
            Branch branch = branchRepository.findByBranchCode(request.getBranchCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchCode()));

            if (branch.getStatus() != Branch.BranchStatus.ACTIVE) {
                throw new InvalidTransactionException("Cannot transfer account to inactive branch");
            }

            account.setBranch(branch);
            log.info("Account {} transferred to branch {}", account.getAccountNumber(), branch.getBranchName());
        }

        if (request.getInterestRate() != null) {
            account.setInterestRate(request.getInterestRate());
        }
        if (request.getNomineeFirstName() != null) {
            account.setNomineeFirstName(request.getNomineeFirstName());
        }
        if (request.getNomineeLastName() != null) {
            account.setNomineeLastName(request.getNomineeLastName());
        }
        if (request.getNomineeRelationship() != null) {
            account.setNomineeRelationship(request.getNomineeRelationship());
        }
        if (request.getNomineePhone() != null) {
            account.setNomineePhone(request.getNomineePhone());
        }
        if (request.getStatus() != null) {
            account.setStatus(Account.Status.valueOf(request.getStatus().toUpperCase()));
        }
        if (request.getKycStatus() != null) {
            account.setKycStatus(Account.KycStatus.valueOf(request.getKycStatus().toUpperCase()));
        }

        account = accountRepository.save(account);

        log.info("Account updated successfully: {}", account.getAccountNumber());

        return mapToResponseDTO(account);
    }

    // ============================================
// DELETE ACCOUNT (with Authorization)
// ============================================
    @Transactional
    public void deleteAccount(Long id, String jwtToken) {
        log.info("Closing account with ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));

        // Authorization check
        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
            log.warn("Unauthorized delete attempt on account {} by user with role {}",
                    account.getAccountNumber(), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions for this account");
        }

        // Check if account has balance
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidTransactionException("Cannot close account with non-zero balance. Current balance: " + account.getBalance());
        }

        // Soft delete - set status to closed
        account.setStatus(Account.Status.CLOSED);
        accountRepository.save(account);

        log.info("Account closed: {}", account.getAccountNumber());
    }

    // ============================================
// FREEZE ACCOUNT (with Authorization)
// ============================================
    @Transactional
    public AccountResponseDTO freezeAccount(String accountNumber, String jwtToken) {
        log.info("Freezing account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        // Authorization check
        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
            log.warn("Unauthorized freeze attempt on account {} by user with role {}",
                    accountNumber, branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions for this account");
        }

        account.setStatus(Account.Status.FROZEN);
        account = accountRepository.save(account);

        log.info("Account frozen successfully");

        return mapToResponseDTO(account);
    }

    // ============================================
// UNFREEZE ACCOUNT (with Authorization)
// ============================================
    @Transactional
    public AccountResponseDTO unfreezeAccount(String accountNumber, String jwtToken) {
        log.info("Unfreezing account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        // Authorization check
        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
            log.warn("Unauthorized unfreeze attempt on account {} by user with role {}",
                    accountNumber, branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions for this account");
        }

        if (account.getStatus() != Account.Status.FROZEN) {
            throw new InvalidTransactionException("Account is not frozen");
        }

        account.setStatus(Account.Status.ACTIVE);
        account = accountRepository.save(account);

        log.info("Account unfrozen successfully");

        return mapToResponseDTO(account);
    }

    // ============================================
// GET ACCOUNT STATEMENT (with Authorization)
// ============================================

    // ============================================
// GET ACCOUNT STATEMENT (with Authorization)
// ============================================
    public AccountStatementDTO getAccountStatement(AccountStatementRequestDTO request, String jwtToken) {
        log.info("Generating statement for account: {} from {} to {}",
                request.getAccountNumber(), request.getStartDate(), request.getEndDate());

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

        // Authorization check using BranchAuthorizationService
        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
            log.warn("Unauthorized statement access attempt for account {} by user with role {}",
                    request.getAccountNumber(), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions for this account");
        }

        // Extract role for additional branch-specific validation
        String role = branchAuthorizationService.extractRole(jwtToken);

        // Additional explicit check for BRANCH_MANAGER
        if ("BRANCH_MANAGER".equals(role)) {
            Long userBranchId = branchAuthorizationService.extractBranchId(jwtToken);
            if (account.getBranch() == null || !account.getBranch().getId().equals(userBranchId)) {
                log.warn("Branch Manager attempted to access statement for account outside their branch");
                throw new UnauthorizedAccessException(
                        "Access denied: You can only view statements for accounts in your branch");
            }
        }

        // Get transactions within date range
        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(
                account.getId(),
                request.getStartDate(),
                request.getEndDate()
        );

        // Calculate opening balance (balance before first transaction)
        BigDecimal openingBalance = account.getBalance();
        if (!transactions.isEmpty()) {
            // Work backwards from current balance
            for (Transaction txn : transactions) {
                if (txn.getFromAccount() != null && txn.getFromAccount().getId().equals(account.getId())) {
                    openingBalance = openingBalance.add(txn.getTotalAmount());
                } else if (txn.getToAccount() != null && txn.getToAccount().getId().equals(account.getId())) {
                    openingBalance = openingBalance.subtract(txn.getAmount());
                }
            }
        }

        // Calculate totals
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;

        for (Transaction txn : transactions) {
            if (txn.getToAccount() != null && txn.getToAccount().getId().equals(account.getId())) {
                totalCredits = totalCredits.add(txn.getAmount());
            }
            if (txn.getFromAccount() != null && txn.getFromAccount().getId().equals(account.getId())) {
                totalDebits = totalDebits.add(txn.getTotalAmount());
            }
        }

        // Map transactions to DTO
        List<TransactionHistoryDTO> transactionDTOs = transactions.stream()
                .map(txn -> mapToTransactionHistoryDTO(txn, account.getAccountNumber()))
                .collect(Collectors.toList());

        // Build statement DTO
        AccountStatementDTO statement = new AccountStatementDTO();
        statement.setAccountNumber(account.getAccountNumber());
        statement.setAccountType(account.getAccountType());
        statement.setCustomerName(account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName());
        statement.setCustomerEmail(account.getCustomer().getEmail());

        // Add branch information
        if (account.getBranch() != null) {
            statement.setBranchCode(account.getBranch().getBranchCode());
            statement.setBranchName(account.getBranch().getBranchName());
            statement.setBranchAddress(account.getBranch().getAddress() + ", " +
                    account.getBranch().getCity() + ", " +
                    account.getBranch().getState());
        }

        statement.setStatementStartDate(request.getStartDate());
        statement.setStatementEndDate(request.getEndDate());
        statement.setOpeningBalance(openingBalance);
        statement.setClosingBalance(account.getBalance());
        statement.setTotalCredits(totalCredits);
        statement.setTotalDebits(totalDebits);
        statement.setTransactionCount(transactions.size());
        statement.setTransactions(transactionDTOs);

        log.info("Statement generated successfully for account {} with {} transactions",
                account.getAccountNumber(), transactions.size());

        return statement;
    }


//    public AccountStatementDTO getAccountStatement(AccountStatementRequestDTO request, String jwtToken) {
//        log.info("Generating statement for account: {} from {} to {}",
//                request.getAccountNumber(), request.getStartDate(), request.getEndDate());
//
//        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
//                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));
//
//        // Authorization check
//        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
//            log.warn("Unauthorized statement access attempt for account {} by user with role {}",
//                    request.getAccountNumber(), branchAuthorizationService.extractRole(jwtToken));
//            throw new UnauthorizedAccessException("Access denied: insufficient permissions for this account");
//        }
//
//        // Get transactions within date range
//        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(
//                account.getId(),
//                request.getStartDate(),
//                request.getEndDate()
//        );
//
//        // Calculate opening balance
//        BigDecimal openingBalance = account.getBalance();
//        if (!transactions.isEmpty()) {
//            for (Transaction txn : transactions) {
//                if (txn.getFromAccount() != null && txn.getFromAccount().getId().equals(account.getId())) {
//                    openingBalance = openingBalance.add(txn.getTotalAmount());
//                } else if (txn.getToAccount() != null && txn.getToAccount().getId().equals(account.getId())) {
//                    openingBalance = openingBalance.subtract(txn.getAmount());
//                }
//            }
//        }
//
//        // Calculate totals
//        BigDecimal totalCredits = BigDecimal.ZERO;
//        BigDecimal totalDebits = BigDecimal.ZERO;
//
//        for (Transaction txn : transactions) {
//            if (txn.getToAccount() != null && txn.getToAccount().getId().equals(account.getId())) {
//                totalCredits = totalCredits.add(txn.getAmount());
//            }
//            if (txn.getFromAccount() != null && txn.getFromAccount().getId().equals(account.getId())) {
//                totalDebits = totalDebits.add(txn.getTotalAmount());
//            }
//        }
//
//        // Map transactions to DTO
//        List<TransactionHistoryDTO> transactionDTOs = transactions.stream()
//                .map(txn -> mapToTransactionHistoryDTO(txn, account.getAccountNumber()))
//                .collect(Collectors.toList());
//
//        // Build statement DTO
//        AccountStatementDTO statement = new AccountStatementDTO();
//        statement.setAccountNumber(account.getAccountNumber());
//        statement.setAccountType(account.getAccountType());
//        statement.setCustomerName(account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName());
//        statement.setCustomerEmail(account.getCustomer().getEmail());
//
//        // Add branch information
//        if (account.getBranch() != null) {
//            statement.setBranchCode(account.getBranch().getBranchCode());
//            statement.setBranchName(account.getBranch().getBranchName());
//            statement.setBranchAddress(account.getBranch().getAddress() + ", " +
//                    account.getBranch().getCity() + ", " +
//                    account.getBranch().getState());
//        }
//
//        statement.setStatementStartDate(request.getStartDate());
//        statement.setStatementEndDate(request.getEndDate());
//        statement.setOpeningBalance(openingBalance);
//        statement.setClosingBalance(account.getBalance());
//        statement.setTotalCredits(totalCredits);
//        statement.setTotalDebits(totalDebits);
//        statement.setTransactionCount(transactions.size());
//        statement.setTransactions(transactionDTOs);
//
//        return statement;
//    }




    // ============================================
// GET ACCOUNTS BY CUSTOMER ID (with Authorization)
// ============================================
    public List<AccountListItemDTO> getAccountsByCustomerId(String customerId, String jwtToken) {
        String role = branchAuthorizationService.extractRole(jwtToken);

        // CUSTOMER can only access their own accounts
        if ("CUSTOMER".equals(role)) {
            String tokenCustomerId = branchAuthorizationService.extractCustomerId(jwtToken);
            if (!customerId.equals(tokenCustomerId)) {
                log.warn("Customer {} attempted to access accounts of customer {}", tokenCustomerId, customerId);
                throw new UnauthorizedAccessException("Access denied: can only access your own accounts");
            }
        }

        // BRANCH_MANAGER can only access accounts in their branch
        if ("BRANCH_MANAGER".equals(role)) {
            Long tokenBranchId = branchAuthorizationService.extractBranchId(jwtToken);
            return accountRepository.findByCustomerId(customerId).stream()
                    .filter(account -> {
                        Long accountBranchId = account.getBranch() != null ? account.getBranch().getId() : null;
                        return tokenBranchId != null && tokenBranchId.equals(accountBranchId);
                    })
                    .map(this::mapToListItemDTO)
                    .collect(Collectors.toList());
        }

        // ADMIN has full access
        return accountRepository.findByCustomerId(customerId).stream()
                .map(this::mapToListItemDTO)
                .collect(Collectors.toList());
    }

    // ============================================
// GET ALL ACCOUNTS (with Authorization Filter)
// ============================================
    public List<AccountListItemDTO> getAllAccounts(String jwtToken) {
        String role = branchAuthorizationService.extractRole(jwtToken);

        if ("ADMIN".equals(role)) {
            // ADMIN: return all accounts
            return accountRepository.findAll().stream()
                    .map(this::mapToListItemDTO)
                    .collect(Collectors.toList());
        } else if ("LOAN_OFFICER".equals(role) || "BRANCH_MANAGER".equals(role)) {
            // LOAN_OFFICER and BRANCH_MANAGER: filter by branchId
            Long branchId = branchAuthorizationService.extractBranchId(jwtToken);
            if (branchId == null) {
                log.warn("{} has no assigned branch", role);
                return List.of();
            }

            return accountRepository.findAll().stream()
                    .filter(account -> {
                        Long accountBranchId = account.getBranch() != null ?
                                account.getBranch().getId() : null;
                        return branchId.equals(accountBranchId);
                    })
                    .map(this::mapToListItemDTO)
                    .collect(Collectors.toList());
        } else if ("CUSTOMER".equals(role)) {
            // CUSTOMER: filter by customerId
            String customerId = branchAuthorizationService.extractCustomerId(jwtToken);
            if (customerId == null) {
                log.warn("Customer ID not found in token");
                return List.of();
            }

            return accountRepository.findByCustomerId(customerId).stream()
                    .map(this::mapToListItemDTO)
                    .collect(Collectors.toList());
        }

        // Other roles have no access
        log.warn("Role {} attempted to access all accounts - denied", role);
        return List.of();
    }






//    public List<AccountListItemDTO> getAllAccounts(String jwtToken) {
//        String role = branchAuthorizationService.extractRole(jwtToken);
//
//        // ADMIN gets all accounts
//        if ("ADMIN".equals(role)) {
//            return accountRepository.findAll().stream()
//                    .map(this::mapToListItemDTO)
//                    .collect(Collectors.toList());
//        }
//
//        // BRANCH_MANAGER gets only accounts in their branch
//        if ("BRANCH_MANAGER".equals(role)) {
//            Long tokenBranchId = branchAuthorizationService.extractBranchId(jwtToken);
//            if (tokenBranchId == null) {
//                log.warn("Branch Manager has no assigned branch");
//                return List.of();
//            }
//
//            return accountRepository.findAll().stream()
//                    .filter(account -> {
//                        Long accountBranchId = account.getBranch() != null ? account.getBranch().getId() : null;
//                        return tokenBranchId.equals(accountBranchId);
//                    })
//                    .map(this::mapToListItemDTO)
//                    .collect(Collectors.toList());
//        }
//
//        // CUSTOMER gets only their own accounts
//        if ("CUSTOMER".equals(role)) {
//            String customerId = branchAuthorizationService.extractCustomerId(jwtToken);
//            return accountRepository.findByCustomerId(customerId).stream()
//                    .map(this::mapToListItemDTO)
//                    .collect(Collectors.toList());
//        }
//
//        // Other roles have no access
//        log.warn("Role {} attempted to access all accounts - denied", role);
//        return List.of();
//    }



    // ============================================
// TRANSFER ACCOUNT TO BRANCH (ADMIN only)
// ============================================
    @Transactional
    public AccountResponseDTO transferAccountToBranch(Long accountId, String targetBranchCode, String jwtToken) {
        log.info("Attempting to transfer account {} to branch {}", accountId, targetBranchCode);

        // 1. Extract and verify role - Only ADMIN can transfer accounts
        String role = branchAuthorizationService.extractRole(jwtToken);
        if (!"ADMIN".equals(role)) {
            log.warn("Unauthorized transfer attempt by user with role: {}", role);
            throw new UnauthorizedAccessException("Access denied: Only ADMIN users can transfer accounts between branches");
        }

        // 2. Fetch the account
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        // 3. Verify target branch exists and is ACTIVE
        Branch targetBranch = branchRepository.findByBranchCode(targetBranchCode)
                .orElseThrow(() -> new ResourceNotFoundException("Target branch not found: " + targetBranchCode));

        if (targetBranch.getStatus() != Branch.BranchStatus.ACTIVE) {
            log.warn("Attempted to transfer account to inactive branch: {}", targetBranchCode);
            throw new InvalidTransactionException("Cannot transfer account to inactive branch: " + targetBranchCode);
        }

        // 4. Check if account is already in the target branch
        if (account.getBranch() != null && account.getBranch().getId().equals(targetBranch.getId())) {
            log.info("Account {} is already in branch {}", account.getAccountNumber(), targetBranchCode);
            throw new InvalidTransactionException("Account is already in the target branch");
        }

        // 5. Store old branch info for logging
        String oldBranchCode = account.getBranch() != null ? account.getBranch().getBranchCode() : "N/A";
        String oldBranchName = account.getBranch() != null ? account.getBranch().getBranchName() : "N/A";

        // 6. Update account branch
        account.setBranch(targetBranch);
        account = accountRepository.save(account);

        // 7. Log the transfer
        log.info("Account transfer completed successfully: Account {} transferred from branch {} ({}) to branch {} ({})",
                account.getAccountNumber(),
                oldBranchCode,
                oldBranchName,
                targetBranch.getBranchCode(),
                targetBranch.getBranchName());

        return mapToResponseDTO(account);
    }



}
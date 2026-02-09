package com.izak.demoBankManagement.service;

import com.izak.demoBankManagement.entity.*;
import com.izak.demoBankManagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for handling branch-level and role-based authorization checks.
 * Determines whether a user (identified by JWT token) has access to specific entities
 * based on their role and branch assignment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BranchAuthorizationService {

    private final JwtUtil jwtUtil;

    /**
     * Check if user can access a specific account
     *
     * @param jwtToken JWT token containing user credentials
     * @param account Account entity to check access for
     * @return true if user has access, false otherwise
     */
    public boolean canAccessAccount(String jwtToken, Account account) {
        if (account == null) {
            log.warn("Cannot authorize access to null account");
            return false;
        }

        try {
            String role = jwtUtil.extractRole(jwtToken);

            // ADMIN has system-wide access
            if ("ADMIN".equals(role)) {
                return true;
            }

            // CUSTOMER can only access their own accounts
            if ("CUSTOMER".equals(role)) {
                String tokenCustomerId = jwtUtil.extractCustomerId(jwtToken);
                boolean hasAccess = account.getCustomerId().equals(tokenCustomerId);
                log.debug("Customer {} access to account {}: {}", tokenCustomerId, account.getAccountNumber(), hasAccess);
                return hasAccess;
            }

            // BRANCH_MANAGER can access accounts in their branch
            if ("BRANCH_MANAGER".equals(role)) {
                Long tokenBranchId = jwtUtil.extractBranchId(jwtToken);
                if (tokenBranchId == null) {
                    log.warn("Branch Manager has no assigned branch in token");
                    return false;
                }

                Long accountBranchId = account.getBranch() != null ? account.getBranch().getId() : null;
                boolean hasAccess = tokenBranchId.equals(accountBranchId);
                log.debug("Branch Manager (branch {}) access to account {} (branch {}): {}",
                        tokenBranchId, account.getAccountNumber(), accountBranchId, hasAccess);
                return hasAccess;
            }

            // LOAN_OFFICER and CARD_OFFICER don't have direct account access
            // unless they need to view account for loan/card operations
            if ("LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
                log.debug("{} attempted to access account {} - no direct access", role, account.getAccountNumber());
                return false;
            }

            log.warn("Unknown role {} attempted to access account", role);
            return false;

        } catch (Exception e) {
            log.error("Error checking account access authorization", e);
            return false;
        }
    }

    /**
     * Check if user can access a specific loan
     *
     * @param jwtToken JWT token containing user credentials
     * @param loan Loan entity to check access for
     * @return true if user has access, false otherwise
     */
    public boolean canAccessLoan(String jwtToken, Loan loan) {
        if (loan == null) {
            log.warn("Cannot authorize access to null loan");
            return false;
        }

        try {
            String role = jwtUtil.extractRole(jwtToken);

            // ADMIN has system-wide access
            if ("ADMIN".equals(role)) {
                return true;
            }

            // CUSTOMER can only access their own loans
            if ("CUSTOMER".equals(role)) {
                String tokenCustomerId = jwtUtil.extractCustomerId(jwtToken);
                String loanCustomerId = loan.getCustomer() != null ? loan.getCustomer().getCustomerId() : null;
                boolean hasAccess = tokenCustomerId != null && tokenCustomerId.equals(loanCustomerId);
                log.debug("Customer {} access to loan {}: {}", tokenCustomerId, loan.getLoanId(), hasAccess);
                return hasAccess;
            }

            // BRANCH_MANAGER and LOAN_OFFICER can access loans in their branch
            if ("BRANCH_MANAGER".equals(role) || "LOAN_OFFICER".equals(role)) {
                Long tokenBranchId = jwtUtil.extractBranchId(jwtToken);
                if (tokenBranchId == null) {
                    log.warn("{} has no assigned branch in token", role);
                    return false;
                }

                // Get branch from loan's account
                Long loanBranchId = null;
                if (loan.getAccount() != null && loan.getAccount().getBranch() != null) {
                    loanBranchId = loan.getAccount().getBranch().getId();
                }

                boolean hasAccess = tokenBranchId.equals(loanBranchId);
                log.debug("{} (branch {}) access to loan {} (branch {}): {}",
                        role, tokenBranchId, loan.getLoanId(), loanBranchId, hasAccess);
                return hasAccess;
            }

            // CARD_OFFICER doesn't have loan access
            if ("CARD_OFFICER".equals(role)) {
                log.debug("Card Officer attempted to access loan {} - no access", loan.getLoanId());
                return false;
            }

            log.warn("Unknown role {} attempted to access loan", role);
            return false;

        } catch (Exception e) {
            log.error("Error checking loan access authorization", e);
            return false;
        }
    }

    /**
     * Check if user can access a specific card
     *
     * @param jwtToken JWT token containing user credentials
     * @param card Card entity to check access for
     * @return true if user has access, false otherwise
     */
    public boolean canAccessCard(String jwtToken, Card card) {
        if (card == null) {
            log.warn("Cannot authorize access to null card");
            return false;
        }

        try {
            String role = jwtUtil.extractRole(jwtToken);

            // ADMIN has system-wide access
            if ("ADMIN".equals(role)) {
                return true;
            }

            // CUSTOMER can only access their own cards
            if ("CUSTOMER".equals(role)) {
                String tokenCustomerId = jwtUtil.extractCustomerId(jwtToken);
                String cardCustomerId = card.getCustomer() != null ? card.getCustomer().getCustomerId() : null;
                boolean hasAccess = tokenCustomerId != null && tokenCustomerId.equals(cardCustomerId);
                log.debug("Customer {} access to card {}: {}", tokenCustomerId, card.getCardNumber(), hasAccess);
                return hasAccess;
            }

            // BRANCH_MANAGER and CARD_OFFICER can access cards in their branch
            if ("BRANCH_MANAGER".equals(role) || "CARD_OFFICER".equals(role)) {
                Long tokenBranchId = jwtUtil.extractBranchId(jwtToken);
                if (tokenBranchId == null) {
                    log.warn("{} has no assigned branch in token", role);
                    return false;
                }

                // Get branch from card's account
                Long cardBranchId = null;
                if (card.getAccount() != null && card.getAccount().getBranch() != null) {
                    cardBranchId = card.getAccount().getBranch().getId();
                }

                boolean hasAccess = tokenBranchId.equals(cardBranchId);
                log.debug("{} (branch {}) access to card {} (branch {}): {}",
                        role, tokenBranchId, card.getCardNumber(), cardBranchId, hasAccess);
                return hasAccess;
            }

            // LOAN_OFFICER doesn't have card access
            if ("LOAN_OFFICER".equals(role)) {
                log.debug("Loan Officer attempted to access card {} - no access", card.getCardNumber());
                return false;
            }

            log.warn("Unknown role {} attempted to access card", role);
            return false;

        } catch (Exception e) {
            log.error("Error checking card access authorization", e);
            return false;
        }
    }

    /**
     * Check if user can access a specific DPS account
     *
     * @param jwtToken JWT token containing user credentials
     * @param dps DPS entity to check access for
     * @return true if user has access, false otherwise
     */
    public boolean canAccessDPS(String jwtToken, DPS dps) {
        if (dps == null) {
            log.warn("Cannot authorize access to null DPS");
            return false;
        }

        try {
            String role = jwtUtil.extractRole(jwtToken);

            // ADMIN has system-wide access
            if ("ADMIN".equals(role)) {
                return true;
            }

            // CUSTOMER can only access their own DPS accounts
            if ("CUSTOMER".equals(role)) {
                String tokenCustomerId = jwtUtil.extractCustomerId(jwtToken);
                boolean hasAccess = dps.getCustomerId().equals(tokenCustomerId);
                log.debug("Customer {} access to DPS {}: {}", tokenCustomerId, dps.getDpsNumber(), hasAccess);
                return hasAccess;
            }

            // BRANCH_MANAGER can access DPS accounts in their branch
            if ("BRANCH_MANAGER".equals(role)) {
                Long tokenBranchId = jwtUtil.extractBranchId(jwtToken);
                if (tokenBranchId == null) {
                    log.warn("Branch Manager has no assigned branch in token");
                    return false;
                }

                Long dpsBranchId = dps.getBranch() != null ? dps.getBranch().getId() : null;
                boolean hasAccess = tokenBranchId.equals(dpsBranchId);
                log.debug("Branch Manager (branch {}) access to DPS {} (branch {}): {}",
                        tokenBranchId, dps.getDpsNumber(), dpsBranchId, hasAccess);
                return hasAccess;
            }

            // LOAN_OFFICER and CARD_OFFICER don't have DPS access
            if ("LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
                log.debug("{} attempted to access DPS {} - no access", role, dps.getDpsNumber());
                return false;
            }

            log.warn("Unknown role {} attempted to access DPS", role);
            return false;

        } catch (Exception e) {
            log.error("Error checking DPS access authorization", e);
            return false;
        }
    }

    /**
     * Check if user can access a specific branch
     *
     * @param jwtToken JWT token containing user credentials
     * @param branchId Branch ID to check access for
     * @return true if user has access, false otherwise
     */
    public boolean canAccessBranch(String jwtToken, Long branchId) {
        if (branchId == null) {
            log.warn("Cannot authorize access to null branch ID");
            return false;
        }

        try {
            String role = jwtUtil.extractRole(jwtToken);

            // ADMIN has system-wide access to all branches
            if ("ADMIN".equals(role)) {
                return true;
            }

            // CUSTOMER doesn't have direct branch access
            if ("CUSTOMER".equals(role)) {
                log.debug("Customer attempted to access branch {} - no direct access", branchId);
                return false;
            }

            // BRANCH_MANAGER, LOAN_OFFICER, and CARD_OFFICER can only access their assigned branch
            if ("BRANCH_MANAGER".equals(role) || "LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
                Long tokenBranchId = jwtUtil.extractBranchId(jwtToken);
                if (tokenBranchId == null) {
                    log.warn("{} has no assigned branch in token", role);
                    return false;
                }

                boolean hasAccess = tokenBranchId.equals(branchId);
                log.debug("{} (branch {}) access to branch {}: {}",
                        role, tokenBranchId, branchId, hasAccess);
                return hasAccess;
            }

            log.warn("Unknown role {} attempted to access branch", role);
            return false;

        } catch (Exception e) {
            log.error("Error checking branch access authorization", e);
            return false;
        }
    }

    /**
     * Check if user can access a transaction
     * Helper method for transaction authorization
     *
     * @param jwtToken JWT token containing user credentials
     * @param transaction Transaction entity to check access for
     * @return true if user has access, false otherwise
     */
    public boolean canAccessTransaction(String jwtToken, Transaction transaction) {
        if (transaction == null) {
            log.warn("Cannot authorize access to null transaction");
            return false;
        }

        try {
            String role = jwtUtil.extractRole(jwtToken);

            // ADMIN has system-wide access
            if ("ADMIN".equals(role)) {
                return true;
            }

            // CUSTOMER can access transactions from their accounts
            if ("CUSTOMER".equals(role)) {
                String tokenCustomerId = jwtUtil.extractCustomerId(jwtToken);

                // Check if customer owns fromAccount or toAccount
                boolean ownsFromAccount = transaction.getFromAccount() != null &&
                        transaction.getFromAccount().getCustomerId().equals(tokenCustomerId);
                boolean ownsToAccount = transaction.getToAccount() != null &&
                        transaction.getToAccount().getCustomerId().equals(tokenCustomerId);

                boolean hasAccess = ownsFromAccount || ownsToAccount;
                log.debug("Customer {} access to transaction {}: {}",
                        tokenCustomerId, transaction.getTransactionId(), hasAccess);
                return hasAccess;
            }

            // BRANCH_MANAGER can access transactions from accounts in their branch
            if ("BRANCH_MANAGER".equals(role)) {
                Long tokenBranchId = jwtUtil.extractBranchId(jwtToken);
                if (tokenBranchId == null) {
                    log.warn("Branch Manager has no assigned branch in token");
                    return false;
                }

                // Check if either account belongs to the branch
                Long fromBranchId = transaction.getFromAccount() != null &&
                        transaction.getFromAccount().getBranch() != null ?
                        transaction.getFromAccount().getBranch().getId() : null;

                Long toBranchId = transaction.getToAccount() != null &&
                        transaction.getToAccount().getBranch() != null ?
                        transaction.getToAccount().getBranch().getId() : null;

                boolean hasAccess = tokenBranchId.equals(fromBranchId) || tokenBranchId.equals(toBranchId);
                log.debug("Branch Manager (branch {}) access to transaction {}: {}",
                        tokenBranchId, transaction.getTransactionId(), hasAccess);
                return hasAccess;
            }

            // LOAN_OFFICER and CARD_OFFICER don't have direct transaction access
            if ("LOAN_OFFICER".equals(role) || "CARD_OFFICER".equals(role)) {
                log.debug("{} attempted to access transaction {} - no direct access",
                        role, transaction.getTransactionId());
                return false;
            }

            log.warn("Unknown role {} attempted to access transaction", role);
            return false;

        } catch (Exception e) {
            log.error("Error checking transaction access authorization", e);
            return false;
        }
    }

    /**
     * Extract and validate user role from JWT token
     *
     * @param jwtToken JWT token
     * @return User role as string, or null if invalid
     */
    public String extractRole(String jwtToken) {
        try {
            return jwtUtil.extractRole(jwtToken);
        } catch (Exception e) {
            log.error("Error extracting role from token", e);
            return null;
        }
    }

    /**
     * Extract customer ID from JWT token
     *
     * @param jwtToken JWT token
     * @return Customer ID, or null if not present/invalid
     */
    public String extractCustomerId(String jwtToken) {
        try {
            return jwtUtil.extractCustomerId(jwtToken);
        } catch (Exception e) {
            log.error("Error extracting customer ID from token", e);
            return null;
        }
    }

    /**
     * Extract branch ID from JWT token
     *
     * @param jwtToken JWT token
     * @return Branch ID, or null if not present/invalid
     */
    public Long extractBranchId(String jwtToken) {
        try {
            return jwtUtil.extractBranchId(jwtToken);
        } catch (Exception e) {
            log.error("Error extracting branch ID from token", e);
            return null;
        }
    }
}
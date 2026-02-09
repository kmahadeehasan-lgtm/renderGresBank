package com.izak.demoBankManagement.service;

import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.entity.Account;
import com.izak.demoBankManagement.entity.Card;
import com.izak.demoBankManagement.entity.Customer;
import com.izak.demoBankManagement.exception.*;
import com.izak.demoBankManagement.repository.AccountRepository;
import com.izak.demoBankManagement.repository.CardRepository;
import com.izak.demoBankManagement.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchAuthorizationService branchAuthorizationService;

    private static final int CARD_VALIDITY_YEARS = 3;
    private static final String VISA_PREFIX = "4532";

    // ============================================
    // ISSUE CARD (with Authorization)
    // ============================================
    @Override
    @Transactional
    public CardResponseDTO issueCard(CardIssueRequestDTO request, String jwtToken) {
        log.info("Issuing new card for customer: {}", request.getCustomerId());

        // Validate customer
        Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + request.getCustomerId()));

        if (customer.getStatus() != Customer.Status.ACTIVE) {
            throw new InvalidCardOperationException("Cannot issue card for inactive customer");
        }

        // Validate account
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + request.getAccountId()));

        if (account.getStatus() != Account.Status.ACTIVE) {
            throw new InvalidCardOperationException("Cannot issue card for inactive account");
        }

        // Verify account belongs to customer
        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw new InvalidCardOperationException("Account does not belong to the specified customer");
        }

        // Authorization check - verify user can access the account
        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
            String role = branchAuthorizationService.extractRole(jwtToken);
            log.warn("Unauthorized card issuance attempt for account {} by user with role {}",
                    account.getAccountNumber(), role);
            throw new UnauthorizedAccessException("Access denied: insufficient permissions to issue card for this account");
        }

        // Validate card type specific requirements
        Card.CardType cardType = Card.CardType.valueOf(request.getCardType().toUpperCase());
        if (cardType == Card.CardType.CREDIT_CARD) {
            if (request.getCreditLimit() == null || request.getCreditLimit().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidCardOperationException("Credit limit is required and must be greater than 0 for credit cards");
            }
        }

        // Generate temporary PIN
        String temporaryPin = generatePIN();

        // Create card
        Card card = new Card();
        card.setCardNumber(generateCardNumber());
        card.setCustomer(customer);
        card.setAccount(account);
        card.setCardType(cardType);
        card.setCardHolderName(customer.getFirstName().toUpperCase() + " " + customer.getLastName().toUpperCase());
        card.setIssueDate(LocalDate.now());
        card.setExpiryDate(LocalDate.now().plusYears(CARD_VALIDITY_YEARS));
        card.setCvv(generateCVV());
        card.setPin(passwordEncoder.encode(temporaryPin)); // Store hashed PIN
        card.setStatus(Card.Status.INACTIVE); // Card starts as INACTIVE until activated
        card.setIsInternational(request.getIsInternational());
        card.setIsOnlinePurchaseEnabled(request.getIsOnlinePurchaseEnabled());
        card.setIsContactless(request.getIsContactless());

        // Set credit card specific fields
        if (cardType == Card.CardType.CREDIT_CARD) {
            card.setCreditLimit(request.getCreditLimit());
            card.setAvailableLimit(request.getCreditLimit());
            card.setOutstandingBalance(BigDecimal.ZERO);
        }

        card = cardRepository.save(card);

        log.info("Card issued successfully: {}", maskCardNumber(card.getCardNumber()));

        // Map to response DTO and include the temporary PIN
        CardResponseDTO responseDTO = mapToResponseDTO(card);
        responseDTO.setTemporaryPin(temporaryPin); // Only set during issuance

        return responseDTO;
    }

    // ============================================
    // GET CARD BY ID (with Authorization)
    // ============================================
    @Override
    public CardResponseDTO getCardById(Long id, String jwtToken) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));

        // Authorization check
        if (!branchAuthorizationService.canAccessCard(jwtToken, card)) {
            log.warn("Unauthorized access attempt to card {} by user with role {}",
                    maskCardNumber(card.getCardNumber()), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions for this card");
        }

        return mapToResponseDTO(card);
    }

    // ============================================
    // GET CARD BY CARD NUMBER (with Authorization)
    // ============================================
    @Override
    public CardResponseDTO getCardByCardNumber(String cardNumber, String jwtToken) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + maskCardNumber(cardNumber)));

        // Authorization check
        if (!branchAuthorizationService.canAccessCard(jwtToken, card)) {
            log.warn("Unauthorized access attempt to card {} by user with role {}",
                    maskCardNumber(cardNumber), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions for this card");
        }

        return mapToResponseDTO(card);
    }

    // ============================================
    // GET ALL CARDS (with Authorization Filter)
    // ============================================
    @Override
    public List<CardListItemDTO> getAllCards(String jwtToken) {
        String role = branchAuthorizationService.extractRole(jwtToken);
        List<Card> cards = cardRepository.findAll();

        if ("ADMIN".equals(role)) {
            // ADMIN: return all cards
            return cards.stream()
                    .map(this::mapToListItemDTO)
                    .collect(Collectors.toList());
        } else if ("LOAN_OFFICER".equals(role) || "BRANCH_MANAGER".equals(role) || "CARD_OFFICER".equals(role)) {
            // LOAN_OFFICER, BRANCH_MANAGER, CARD_OFFICER: filter by branchId
            Long branchId = branchAuthorizationService.extractBranchId(jwtToken);
            if (branchId == null) {
                log.warn("{} has no assigned branch", role);
                return List.of();
            }

            return cards.stream()
                    .filter(card -> {
                        // Check if card's account belongs to the same branch
                        if (card.getAccount() != null && card.getAccount().getBranch() != null) {
                            return branchId.equals(card.getAccount().getBranch().getId());
                        }
                        return false;
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

            return cards.stream()
                    .filter(card -> card.getCustomer() != null &&
                            customerId.equals(card.getCustomer().getCustomerId()))
                    .map(this::mapToListItemDTO)
                    .collect(Collectors.toList());
        }

        // Other roles have no access
        log.warn("Role {} attempted to access all cards - denied", role);
        return List.of();
    }
















//    @Override
//    public List<CardListItemDTO> getAllCards(String jwtToken) {
//        String role = branchAuthorizationService.extractRole(jwtToken);
//        List<Card> cards = cardRepository.findAll();
//
//        // Filter based on role
//        if ("ADMIN".equals(role)) {
//            // ADMIN gets all cards
//            return cards.stream()
//                    .map(this::mapToListItemDTO)
//                    .collect(Collectors.toList());
//        } else if ("BRANCH_MANAGER".equals(role) || "CARD_OFFICER".equals(role)) {
//            // Filter cards by branch
//            return cards.stream()
//                    .filter(card -> branchAuthorizationService.canAccessCard(jwtToken, card))
//                    .map(this::mapToListItemDTO)
//                    .collect(Collectors.toList());
//        } else if ("CUSTOMER".equals(role)) {
//            // Filter cards by customer
//            String customerId = branchAuthorizationService.extractCustomerId(jwtToken);
//            return cards.stream()
//                    .filter(card -> card.getCustomer() != null &&
//                            customerId.equals(card.getCustomer().getCustomerId()))
//                    .map(this::mapToListItemDTO)
//                    .collect(Collectors.toList());
//        }
//
//        // Other roles have no card access
//        log.warn("Role {} attempted to access all cards - denied", role);
//        return List.of();
//    }

    // ============================================
    // GET CARDS BY CUSTOMER ID (with Authorization)
    // ============================================
    @Override
    public List<CardListItemDTO> getCardsByCustomerId(String customerId, String jwtToken) {
        String role = branchAuthorizationService.extractRole(jwtToken);

        // CUSTOMER can only access their own cards
        if ("CUSTOMER".equals(role)) {
            String tokenCustomerId = branchAuthorizationService.extractCustomerId(jwtToken);
            if (!customerId.equals(tokenCustomerId)) {
                log.warn("Customer {} attempted to access cards of customer {}", tokenCustomerId, customerId);
                throw new UnauthorizedAccessException("Access denied: can only access your own cards");
            }
        }

        List<Card> cards = cardRepository.findByCustomerCustomerId(customerId);

        // Filter cards based on branch authorization
        if ("BRANCH_MANAGER".equals(role) || "CARD_OFFICER".equals(role)) {
            cards = cards.stream()
                    .filter(card -> branchAuthorizationService.canAccessCard(jwtToken, card))
                    .collect(Collectors.toList());
        }

        return cards.stream()
                .map(this::mapToListItemDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // GET CARDS BY ACCOUNT ID (with Authorization)
    // ============================================
    @Override
    public List<CardListItemDTO> getCardsByAccountId(Long accountId, String jwtToken) {
        // First verify user can access the account
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        if (!branchAuthorizationService.canAccessAccount(jwtToken, account)) {
            log.warn("Unauthorized access attempt to cards for account {} by user with role {}",
                    account.getAccountNumber(), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions for this account");
        }

        return cardRepository.findByAccountId(accountId).stream()
                .map(this::mapToListItemDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // GET CARDS BY STATUS (with Authorization Filter)
    // ============================================
    @Override
    public List<CardListItemDTO> getCardsByStatus(String status, String jwtToken) {
        Card.Status cardStatus = Card.Status.valueOf(status.toUpperCase());
        List<Card> cards = cardRepository.findByStatus(cardStatus);

        String role = branchAuthorizationService.extractRole(jwtToken);

        // Filter based on role and branch
        if ("BRANCH_MANAGER".equals(role) || "CARD_OFFICER".equals(role)) {
            cards = cards.stream()
                    .filter(card -> branchAuthorizationService.canAccessCard(jwtToken, card))
                    .collect(Collectors.toList());
        } else if ("CUSTOMER".equals(role)) {
            String customerId = branchAuthorizationService.extractCustomerId(jwtToken);
            cards = cards.stream()
                    .filter(card -> card.getCustomer() != null &&
                            customerId.equals(card.getCustomer().getCustomerId()))
                    .collect(Collectors.toList());
        } else if (!"ADMIN".equals(role)) {
            // Other roles have no access
            return List.of();
        }

        return cards.stream()
                .map(this::mapToListItemDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // GET CARDS EXPIRING WITHIN 30 DAYS (with Authorization Filter)
    // ============================================
    @Override
    public List<CardListItemDTO> getCardsExpiringWithin30Days(String jwtToken) {
        LocalDate thirtyDaysLater = LocalDate.now().plusDays(30);
        List<Card> cards = cardRepository.findCardsExpiringWithinDays(thirtyDaysLater);

        String role = branchAuthorizationService.extractRole(jwtToken);

        // Filter based on role and branch
        if ("BRANCH_MANAGER".equals(role) || "CARD_OFFICER".equals(role)) {
            cards = cards.stream()
                    .filter(card -> branchAuthorizationService.canAccessCard(jwtToken, card))
                    .collect(Collectors.toList());
        } else if ("CUSTOMER".equals(role)) {
            String customerId = branchAuthorizationService.extractCustomerId(jwtToken);
            cards = cards.stream()
                    .filter(card -> card.getCustomer() != null &&
                            customerId.equals(card.getCustomer().getCustomerId()))
                    .collect(Collectors.toList());
        } else if (!"ADMIN".equals(role)) {
            // Other roles have no access
            return List.of();
        }

        return cards.stream()
                .map(this::mapToListItemDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // UPDATE CARD STATUS (with Authorization)
    // ============================================
    @Override
    @Transactional
    public CardResponseDTO updateCardStatus(Long id, CardStatusUpdateDTO request, String jwtToken) {
        log.info("Updating card status for ID: {}", id);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));

        // Authorization check
        if (!branchAuthorizationService.canAccessCard(jwtToken, card)) {
            log.warn("Unauthorized card status update attempt for card {} by user with role {}",
                    maskCardNumber(card.getCardNumber()), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions to update this card");
        }

        Card.Status newStatus = Card.Status.valueOf(request.getStatus().toUpperCase());

        // Validate status transitions
        if (card.getStatus() == Card.Status.CANCELLED) {
            throw new InvalidCardOperationException("Cannot change status of a cancelled card");
        }

        if (newStatus == Card.Status.CANCELLED && card.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidCardOperationException("Cannot cancel card with outstanding balance: " + card.getOutstandingBalance());
        }

        if (newStatus == Card.Status.BLOCKED || newStatus == Card.Status.CANCELLED) {
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new InvalidCardOperationException("Reason is required for blocking or cancelling a card");
            }
        }

        card.setStatus(newStatus);

        if (newStatus == Card.Status.BLOCKED) {
            card.setBlockDate(LocalDate.now());
            card.setBlockReason(request.getReason());
        } else if (newStatus == Card.Status.ACTIVE) {
            card.setBlockDate(null);
            card.setBlockReason(null);
            if (card.getActivationDate() == null) {
                card.setActivationDate(LocalDate.now());
            }
        }

        card = cardRepository.save(card);

        log.info("Card status updated to: {}", newStatus);

        return mapToResponseDTO(card);
    }

    // ============================================
    // UPDATE CARD PIN (with Authorization)
    // ============================================
    @Override
    @Transactional
    public CardResponseDTO updateCardPin(Long id, CardPinUpdateDTO request, String jwtToken) {
        log.info("Updating PIN for card ID: {}", id);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));

        // Authorization check
        if (!branchAuthorizationService.canAccessCard(jwtToken, card)) {
            log.warn("Unauthorized PIN update attempt for card {} by user with role {}",
                    maskCardNumber(card.getCardNumber()), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions to update PIN for this card");
        }

        if (card.getStatus() == Card.Status.CANCELLED) {
            throw new InvalidCardOperationException("Cannot update PIN of a cancelled card");
        }

        if (card.getStatus() == Card.Status.BLOCKED) {
            throw new InvalidCardOperationException("Cannot update PIN of a blocked card");
        }

        // Verify old PIN
        if (!passwordEncoder.matches(request.getOldPin(), card.getPin())) {
            throw new InvalidCardOperationException("Invalid old PIN");
        }

        // Validate new PIN is different
        if (request.getOldPin().equals(request.getNewPin())) {
            throw new InvalidCardOperationException("New PIN must be different from old PIN");
        }

        // Update PIN
        card.setPin(passwordEncoder.encode(request.getNewPin()));
        card = cardRepository.save(card);

        log.info("PIN updated successfully for card");

        return mapToResponseDTO(card);
    }

    // ============================================
    // UPDATE CARD LIMIT (with Authorization)
    // ============================================
    @Override
    @Transactional
    public CardResponseDTO updateCardLimit(Long id, CardLimitUpdateDTO request, String jwtToken) {
        log.info("Updating credit limit for card ID: {}", id);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));

        // Authorization check
        if (!branchAuthorizationService.canAccessCard(jwtToken, card)) {
            log.warn("Unauthorized limit update attempt for card {} by user with role {}",
                    maskCardNumber(card.getCardNumber()), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions to update limit for this card");
        }

        if (card.getCardType() != Card.CardType.CREDIT_CARD) {
            throw new InvalidCardOperationException("Credit limit can only be updated for credit cards");
        }

        if (card.getStatus() == Card.Status.CANCELLED) {
            throw new InvalidCardOperationException("Cannot update limit of a cancelled card");
        }

        if (request.getCreditLimit().compareTo(card.getOutstandingBalance()) < 0) {
            throw new InvalidCardOperationException("New credit limit cannot be less than outstanding balance: " + card.getOutstandingBalance());
        }

        BigDecimal oldLimit = card.getCreditLimit();
        BigDecimal limitDifference = request.getCreditLimit().subtract(oldLimit);

        card.setCreditLimit(request.getCreditLimit());
        card.setAvailableLimit(card.getAvailableLimit().add(limitDifference));

        card = cardRepository.save(card);

        log.info("Credit limit updated from {} to {}", oldLimit, request.getCreditLimit());

        return mapToResponseDTO(card);
    }

    // ============================================
    // ACTIVATE CARD (with Authorization)
    // ============================================
    @Override
    @Transactional
    public CardResponseDTO activateCard(Long id, String jwtToken) {
        log.info("Activating card ID: {}", id);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));

        // Authorization check
        if (!branchAuthorizationService.canAccessCard(jwtToken, card)) {
            log.warn("Unauthorized activation attempt for card {} by user with role {}",
                    maskCardNumber(card.getCardNumber()), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions to activate this card");
        }

        if (card.getStatus() == Card.Status.ACTIVE) {
            throw new InvalidCardOperationException("Card is already active");
        }

        if (card.getStatus() == Card.Status.CANCELLED) {
            throw new InvalidCardOperationException("Cannot activate a cancelled card");
        }

        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CardExpiredException("Cannot activate an expired card");
        }

        card.setStatus(Card.Status.ACTIVE);
        card.setActivationDate(LocalDate.now());

        card = cardRepository.save(card);

        log.info("Card activated successfully");

        return mapToResponseDTO(card);
    }

    // ============================================
    // BLOCK CARD (with Authorization)
    // ============================================
    @Override
    @Transactional
    public CardResponseDTO blockCard(Long id, String reason, String jwtToken) {
        log.info("Blocking card ID: {}", id);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));

        // Authorization check
        if (!branchAuthorizationService.canAccessCard(jwtToken, card)) {
            log.warn("Unauthorized block attempt for card {} by user with role {}",
                    maskCardNumber(card.getCardNumber()), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions to block this card");
        }

        if (card.getStatus() == Card.Status.CANCELLED) {
            throw new InvalidCardOperationException("Cannot block a cancelled card");
        }

        if (card.getStatus() == Card.Status.BLOCKED) {
            throw new InvalidCardOperationException("Card is already blocked");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new InvalidCardOperationException("Reason is required for blocking a card");
        }

        card.setStatus(Card.Status.BLOCKED);
        card.setBlockDate(LocalDate.now());
        card.setBlockReason(reason);

        card = cardRepository.save(card);

        log.info("Card blocked successfully");

        return mapToResponseDTO(card);
    }

    // ============================================
    // UNBLOCK CARD (with Authorization)
    // ============================================
    @Override
    @Transactional
    public CardResponseDTO unblockCard(Long id, String jwtToken) {
        log.info("Unblocking card ID: {}", id);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));

        // Authorization check
        if (!branchAuthorizationService.canAccessCard(jwtToken, card)) {
            log.warn("Unauthorized unblock attempt for card {} by user with role {}",
                    maskCardNumber(card.getCardNumber()), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions to unblock this card");
        }

        if (card.getStatus() != Card.Status.BLOCKED) {
            throw new InvalidCardOperationException("Card is not blocked");
        }

        // Check if card is expired
        if (card.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CardExpiredException("Cannot unblock an expired card");
        }

        card.setStatus(Card.Status.ACTIVE);
        card.setBlockDate(null);
        card.setBlockReason(null);

        card = cardRepository.save(card);

        log.info("Card unblocked successfully");

        return mapToResponseDTO(card);
    }

    // ============================================
    // CANCEL CARD (with Authorization)
    // ============================================
    @Override
    @Transactional
    public void cancelCard(Long id, String reason, String jwtToken) {
        log.info("Cancelling card ID: {}", id);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));

        // Authorization check
        if (!branchAuthorizationService.canAccessCard(jwtToken, card)) {
            log.warn("Unauthorized cancellation attempt for card {} by user with role {}",
                    maskCardNumber(card.getCardNumber()), branchAuthorizationService.extractRole(jwtToken));
            throw new UnauthorizedAccessException("Access denied: insufficient permissions to cancel this card");
        }

        if (card.getStatus() == Card.Status.CANCELLED) {
            throw new InvalidCardOperationException("Card is already cancelled");
        }

        if (card.getCardType() == Card.CardType.CREDIT_CARD &&
                card.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidCardOperationException("Cannot cancel card with outstanding balance: " + card.getOutstandingBalance());
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new InvalidCardOperationException("Reason is required for cancelling a card");
        }

        card.setStatus(Card.Status.CANCELLED);
        card.setBlockReason(reason);
        card.setBlockDate(LocalDate.now());

        cardRepository.save(card);

        log.info("Card cancelled successfully");
    }

    // ============================================
    // HELPER METHODS (unchanged)
    // ============================================

    private String generateCardNumber() {
        String cardNumber;
        do {
            // Generate Visa format: 4532 XXXX XXXX XXXX
            StringBuilder sb = new StringBuilder(VISA_PREFIX);
            Random random = new Random();
            for (int i = 0; i < 12; i++) {
                sb.append(random.nextInt(10));
            }
            cardNumber = sb.toString();
        } while (cardRepository.existsByCardNumber(cardNumber));

        return cardNumber;
    }

    private String generateCVV() {
        Random random = new Random();
        return String.format("%03d", random.nextInt(1000));
    }

    private String generatePIN() {
        Random random = new Random();
        return String.format("%04d", random.nextInt(10000));
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    private CardResponseDTO mapToResponseDTO(Card card) {
        CardResponseDTO dto = new CardResponseDTO();
        dto.setId(card.getId());
        dto.setMaskedCardNumber(maskCardNumber(card.getCardNumber()));
        dto.setCardHolderName(card.getCardHolderName());
        dto.setCardType(card.getCardType().name().toLowerCase());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setStatus(card.getStatus().name().toLowerCase());

        // Customer and Account info
        dto.setCustomerId(card.getCustomer().getCustomerId());
        dto.setCustomerName(card.getCustomer().getFirstName() + " " + card.getCustomer().getLastName());
        dto.setAccountId(card.getAccount().getId());
        dto.setAccountNumber(card.getAccount().getAccountNumber());

        // Card limits and features
        dto.setCreditLimit(card.getCreditLimit());
        dto.setAvailableLimit(card.getAvailableLimit());
        dto.setOutstandingBalance(card.getOutstandingBalance());
        dto.setIsInternational(card.getIsInternational());
        dto.setIsOnlinePurchaseEnabled(card.getIsOnlinePurchaseEnabled());
        dto.setIsContactless(card.getIsContactless());

        // Dates
        dto.setIssueDate(card.getIssueDate());
        dto.setActivationDate(card.getActivationDate());
        dto.setBlockDate(card.getBlockDate());
        dto.setBlockReason(card.getBlockReason());

        // Audit
        dto.setCreatedDate(card.getCreatedDate());
        dto.setLastModified(card.getLastModified());

        return dto;
    }

    private CardListItemDTO mapToListItemDTO(Card card) {
        CardListItemDTO dto = new CardListItemDTO();
        dto.setId(card.getId());
        dto.setMaskedCardNumber(maskCardNumber(card.getCardNumber()));
        dto.setCardHolderName(card.getCardHolderName());
        dto.setCardType(card.getCardType().name().toLowerCase());
        dto.setStatus(card.getStatus().name().toLowerCase());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setCreditLimit(card.getCreditLimit());
        dto.setAvailableLimit(card.getAvailableLimit());
        dto.setCustomerId(card.getCustomer().getCustomerId());
        dto.setAccountNumber(card.getAccount().getAccountNumber());
        dto.setIsInternational(card.getIsInternational());

        return dto;
    }
}



//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class CardServiceImpl implements CardService {
//
//    private final CardRepository cardRepository;
//    private final CustomerRepository customerRepository;
//    private final AccountRepository accountRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    private static final int CARD_VALIDITY_YEARS = 3;
//    private static final String VISA_PREFIX = "4532";
//
//
//    @Override
//    @Transactional
//    public CardResponseDTO issueCard(CardIssueRequestDTO request) {
//        log.info("Issuing new card for customer: {}", request.getCustomerId());
//
//        // Validate customer
//        Customer customer = customerRepository.findByCustomerId(request.getCustomerId())
//                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + request.getCustomerId()));
//
//        if (customer.getStatus() != Customer.Status.ACTIVE) {
//            throw new InvalidCardOperationException("Cannot issue card for inactive customer");
//        }
//
//        // Validate account
//        Account account = accountRepository.findById(request.getAccountId())
//                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + request.getAccountId()));
//
//        if (account.getStatus() != Account.Status.ACTIVE) {
//            throw new InvalidCardOperationException("Cannot issue card for inactive account");
//        }
//
//        // Verify account belongs to customer
//        if (!account.getCustomer().getId().equals(customer.getId())) {
//            throw new InvalidCardOperationException("Account does not belong to the specified customer");
//        }
//
//        // Validate card type specific requirements
//        Card.CardType cardType = Card.CardType.valueOf(request.getCardType().toUpperCase());
//        if (cardType == Card.CardType.CREDIT_CARD) {
//            if (request.getCreditLimit() == null || request.getCreditLimit().compareTo(BigDecimal.ZERO) <= 0) {
//                throw new InvalidCardOperationException("Credit limit is required and must be greater than 0 for credit cards");
//            }
//        }
//
//        // Generate temporary PIN
//        String temporaryPin = generatePIN();
//
//        // Create card
//        Card card = new Card();
//        card.setCardNumber(generateCardNumber());
//        card.setCustomer(customer);
//        card.setAccount(account);
//        card.setCardType(cardType);
//        card.setCardHolderName(customer.getFirstName().toUpperCase() + " " + customer.getLastName().toUpperCase());
//        card.setIssueDate(LocalDate.now());
//        card.setExpiryDate(LocalDate.now().plusYears(CARD_VALIDITY_YEARS));
//        card.setCvv(generateCVV());
//        card.setPin(passwordEncoder.encode(temporaryPin)); // Store hashed PIN
//        card.setStatus(Card.Status.INACTIVE); // Card starts as INACTIVE until activated
//        card.setIsInternational(request.getIsInternational());
//        card.setIsOnlinePurchaseEnabled(request.getIsOnlinePurchaseEnabled());
//        card.setIsContactless(request.getIsContactless());
//
//        // Set credit card specific fields
//        if (cardType == Card.CardType.CREDIT_CARD) {
//            card.setCreditLimit(request.getCreditLimit());
//            card.setAvailableLimit(request.getCreditLimit());
//            card.setOutstandingBalance(BigDecimal.ZERO);
//        }
//
//        card = cardRepository.save(card);
//
//        log.info("Card issued successfully: {}", maskCardNumber(card.getCardNumber()));
//
//        // Map to response DTO and include the temporary PIN
//        CardResponseDTO responseDTO = mapToResponseDTO(card);
//        responseDTO.setTemporaryPin(temporaryPin); // Only set during issuance
//
//        return responseDTO;
//    }
//
//
//    @Override
//    public CardResponseDTO getCardById(Long id) {
//        Card card = cardRepository.findById(id)
//                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));
//        return mapToResponseDTO(card);
//    }
//
//    @Override
//    public CardResponseDTO getCardByCardNumber(String cardNumber) {
//        Card card = cardRepository.findByCardNumber(cardNumber)
//                .orElseThrow(() -> new CardNotFoundException("Card not found: " + maskCardNumber(cardNumber)));
//        return mapToResponseDTO(card);
//    }
//
//    @Override
//    public List<CardListItemDTO> getAllCards() {
//        return cardRepository.findAll().stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<CardListItemDTO> getCardsByCustomerId(String customerId) {
//        return cardRepository.findByCustomerCustomerId(customerId).stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<CardListItemDTO> getCardsByAccountId(Long accountId) {
//        return cardRepository.findByAccountId(accountId).stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<CardListItemDTO> getCardsByStatus(String status) {
//        Card.Status cardStatus = Card.Status.valueOf(status.toUpperCase());
//        return cardRepository.findByStatus(cardStatus).stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<CardListItemDTO> getCardsExpiringWithin30Days() {
//        LocalDate thirtyDaysLater = LocalDate.now().plusDays(30);
//        return cardRepository.findCardsExpiringWithinDays(thirtyDaysLater).stream()
//                .map(this::mapToListItemDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional
//    public CardResponseDTO updateCardStatus(Long id, CardStatusUpdateDTO request) {
//        log.info("Updating card status for ID: {}", id);
//
//        Card card = cardRepository.findById(id)
//                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));
//
//        Card.Status newStatus = Card.Status.valueOf(request.getStatus().toUpperCase());
//
//        // Validate status transitions
//        if (card.getStatus() == Card.Status.CANCELLED) {
//            throw new InvalidCardOperationException("Cannot change status of a cancelled card");
//        }
//
//        if (newStatus == Card.Status.CANCELLED && card.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
//            throw new InvalidCardOperationException("Cannot cancel card with outstanding balance: " + card.getOutstandingBalance());
//        }
//
//        if (newStatus == Card.Status.BLOCKED || newStatus == Card.Status.CANCELLED) {
//            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
//                throw new InvalidCardOperationException("Reason is required for blocking or cancelling a card");
//            }
//        }
//
//        card.setStatus(newStatus);
//
//        if (newStatus == Card.Status.BLOCKED) {
//            card.setBlockDate(LocalDate.now());
//            card.setBlockReason(request.getReason());
//        } else if (newStatus == Card.Status.ACTIVE) {
//            card.setBlockDate(null);
//            card.setBlockReason(null);
//            if (card.getActivationDate() == null) {
//                card.setActivationDate(LocalDate.now());
//            }
//        }
//
//        card = cardRepository.save(card);
//
//        log.info("Card status updated to: {}", newStatus);
//
//        return mapToResponseDTO(card);
//    }
//
//    @Override
//    @Transactional
//    public CardResponseDTO updateCardPin(Long id, CardPinUpdateDTO request) {
//        log.info("Updating PIN for card ID: {}", id);
//
//        Card card = cardRepository.findById(id)
//                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));
//
//        if (card.getStatus() == Card.Status.CANCELLED) {
//            throw new InvalidCardOperationException("Cannot update PIN of a cancelled card");
//        }
//
//        if (card.getStatus() == Card.Status.BLOCKED) {
//            throw new InvalidCardOperationException("Cannot update PIN of a blocked card");
//        }
//
//        // Verify old PIN
//        if (!passwordEncoder.matches(request.getOldPin(), card.getPin())) {
//            throw new InvalidCardOperationException("Invalid old PIN");
//        }
//
//        // Validate new PIN is different
//        if (request.getOldPin().equals(request.getNewPin())) {
//            throw new InvalidCardOperationException("New PIN must be different from old PIN");
//        }
//
//        // Update PIN
//        card.setPin(passwordEncoder.encode(request.getNewPin()));
//        card = cardRepository.save(card);
//
//        log.info("PIN updated successfully for card");
//
//        return mapToResponseDTO(card);
//    }
//
//    @Override
//    @Transactional
//    public CardResponseDTO updateCardLimit(Long id, CardLimitUpdateDTO request) {
//        log.info("Updating credit limit for card ID: {}", id);
//
//        Card card = cardRepository.findById(id)
//                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));
//
//        if (card.getCardType() != Card.CardType.CREDIT_CARD) {
//            throw new InvalidCardOperationException("Credit limit can only be updated for credit cards");
//        }
//
//        if (card.getStatus() == Card.Status.CANCELLED) {
//            throw new InvalidCardOperationException("Cannot update limit of a cancelled card");
//        }
//
//
//        if (request.getCreditLimit().compareTo(card.getOutstandingBalance()) < 0) {
//            throw new InvalidCardOperationException("New credit limit cannot be less than outstanding balance: " + card.getOutstandingBalance());
//        }
//
//        BigDecimal oldLimit = card.getCreditLimit();
//        BigDecimal limitDifference = request.getCreditLimit().subtract(oldLimit);
//
//        card.setCreditLimit(request.getCreditLimit());
//        card.setAvailableLimit(card.getAvailableLimit().add(limitDifference));
//
//        card = cardRepository.save(card);
//
//        log.info("Credit limit updated from {} to {}", oldLimit, request.getCreditLimit());
//
//        return mapToResponseDTO(card);
//    }
//
//    @Override
//    @Transactional
//    public CardResponseDTO activateCard(Long id) {
//        log.info("Activating card ID: {}", id);
//
//        Card card = cardRepository.findById(id)
//                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));
//
//        if (card.getStatus() == Card.Status.ACTIVE) {
//            throw new InvalidCardOperationException("Card is already active");
//        }
//
//        if (card.getStatus() == Card.Status.CANCELLED) {
//            throw new InvalidCardOperationException("Cannot activate a cancelled card");
//        }
//
//
//        if (card.getExpiryDate().isBefore(LocalDate.now())) {
//            throw new CardExpiredException("Cannot activate an expired card");
//        }
//
//        card.setStatus(Card.Status.ACTIVE);
//        card.setActivationDate(LocalDate.now());
//
//        card = cardRepository.save(card);
//
//        log.info("Card activated successfully");
//
//        return mapToResponseDTO(card);
//    }
//
//    @Override
//    @Transactional
//    public CardResponseDTO blockCard(Long id, String reason) {
//        log.info("Blocking card ID: {}", id);
//
//        Card card = cardRepository.findById(id)
//                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));
//
//        if (card.getStatus() == Card.Status.CANCELLED) {
//            throw new InvalidCardOperationException("Cannot block a cancelled card");
//        }
//
//        if (card.getStatus() == Card.Status.BLOCKED) {
//            throw new InvalidCardOperationException("Card is already blocked");
//        }
//
//        if (reason == null || reason.trim().isEmpty()) {
//            throw new InvalidCardOperationException("Reason is required for blocking a card");
//        }
//
//        card.setStatus(Card.Status.BLOCKED);
//        card.setBlockDate(LocalDate.now());
//        card.setBlockReason(reason);
//
//        card = cardRepository.save(card);
//
//        log.info("Card blocked successfully");
//
//        return mapToResponseDTO(card);
//    }
//
//    @Override
//    @Transactional
//    public CardResponseDTO unblockCard(Long id) {
//        log.info("Unblocking card ID: {}", id);
//
//        Card card = cardRepository.findById(id)
//                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));
//
//        if (card.getStatus() != Card.Status.BLOCKED) {
//            throw new InvalidCardOperationException("Card is not blocked");
//        }
//
//        // Check if card is expired
//        if (card.getExpiryDate().isBefore(LocalDate.now())) {
//            throw new CardExpiredException("Cannot unblock an expired card");
//        }
//
//        card.setStatus(Card.Status.ACTIVE);
//        card.setBlockDate(null);
//        card.setBlockReason(null);
//
//        card = cardRepository.save(card);
//
//        log.info("Card unblocked successfully");
//
//        return mapToResponseDTO(card);
//    }
//
//    @Override
//    @Transactional
//    public void cancelCard(Long id, String reason) {
//        log.info("Cancelling card ID: {}", id);
//
//        Card card = cardRepository.findById(id)
//                .orElseThrow(() -> new CardNotFoundException("Card not found with ID: " + id));
//
//        if (card.getStatus() == Card.Status.CANCELLED) {
//            throw new InvalidCardOperationException("Card is already cancelled");
//        }
//
//        if (card.getCardType() == Card.CardType.CREDIT_CARD &&
//                card.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
//            throw new InvalidCardOperationException("Cannot cancel card with outstanding balance: " + card.getOutstandingBalance());
//        }
//
//        if (reason == null || reason.trim().isEmpty()) {
//            throw new InvalidCardOperationException("Reason is required for cancelling a card");
//        }
//
//        card.setStatus(Card.Status.CANCELLED);
//        card.setBlockReason(reason);
//        card.setBlockDate(LocalDate.now());
//
//        cardRepository.save(card);
//
//        log.info("Card cancelled successfully");
//    }
//
//
//
//    private String generateCardNumber() {
//        String cardNumber;
//        do {
//            // Generate Visa format: 4532 XXXX XXXX XXXX
//            StringBuilder sb = new StringBuilder(VISA_PREFIX);
//            Random random = new Random();
//            for (int i = 0; i < 12; i++) {
//                sb.append(random.nextInt(10));
//            }
//            cardNumber = sb.toString();
//        } while (cardRepository.existsByCardNumber(cardNumber));
//
//        return cardNumber;
//    }
//
//    private String generateCVV() {
//        Random random = new Random();
//        return String.format("%03d", random.nextInt(1000));
//    }
//
//    private String generatePIN() {
//        Random random = new Random();
//        return String.format("%04d", random.nextInt(10000));
//    }
//
//    private String maskCardNumber(String cardNumber) {
//        if (cardNumber == null || cardNumber.length() < 4) {
//            return "****";
//        }
//        String lastFour = cardNumber.substring(cardNumber.length() - 4);
//        return "**** **** **** " + lastFour;
//    }
//
//    private CardResponseDTO mapToResponseDTO(Card card) {
//        CardResponseDTO dto = new CardResponseDTO();
//        dto.setId(card.getId());
//        dto.setMaskedCardNumber(maskCardNumber(card.getCardNumber()));
//        dto.setCardHolderName(card.getCardHolderName());
//        dto.setCardType(card.getCardType().name().toLowerCase());
//        dto.setExpiryDate(card.getExpiryDate());
//        dto.setStatus(card.getStatus().name().toLowerCase());
//
//        // Customer and Account info
//        dto.setCustomerId(card.getCustomer().getCustomerId());
//        dto.setCustomerName(card.getCustomer().getFirstName() + " " + card.getCustomer().getLastName());
//        dto.setAccountId(card.getAccount().getId());
//        dto.setAccountNumber(card.getAccount().getAccountNumber());
//
//        // Card limits and features
//        dto.setCreditLimit(card.getCreditLimit());
//        dto.setAvailableLimit(card.getAvailableLimit());
//        dto.setOutstandingBalance(card.getOutstandingBalance());
//        dto.setIsInternational(card.getIsInternational());
//        dto.setIsOnlinePurchaseEnabled(card.getIsOnlinePurchaseEnabled());
//        dto.setIsContactless(card.getIsContactless());
//
//        // Dates
//        dto.setIssueDate(card.getIssueDate());
//        dto.setActivationDate(card.getActivationDate());
//        dto.setBlockDate(card.getBlockDate());
//        dto.setBlockReason(card.getBlockReason());
//
//        // Audit
//        dto.setCreatedDate(card.getCreatedDate());
//        dto.setLastModified(card.getLastModified());
//
//        return dto;
//    }
//
//    private CardListItemDTO mapToListItemDTO(Card card) {
//        CardListItemDTO dto = new CardListItemDTO();
//        dto.setId(card.getId());
//        dto.setMaskedCardNumber(maskCardNumber(card.getCardNumber()));
//        dto.setCardHolderName(card.getCardHolderName());
//        dto.setCardType(card.getCardType().name().toLowerCase());
//        dto.setStatus(card.getStatus().name().toLowerCase());
//        dto.setExpiryDate(card.getExpiryDate());
//        dto.setCreditLimit(card.getCreditLimit());
//        dto.setAvailableLimit(card.getAvailableLimit());
//        dto.setCustomerId(card.getCustomer().getCustomerId());
//        dto.setAccountNumber(card.getAccount().getAccountNumber());
//        dto.setIsInternational(card.getIsInternational());
//
//        return dto;
//    }
//}

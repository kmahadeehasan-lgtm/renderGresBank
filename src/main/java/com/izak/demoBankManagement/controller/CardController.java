package com.izak.demoBankManagement.controller;

import com.izak.demoBankManagement.dto.*;
import com.izak.demoBankManagement.service.CardService;
import com.izak.demoBankManagement.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class CardController {

    private final CardService cardService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
    public ResponseEntity<ApiResponse<CardResponseDTO>> issueCard(
            @Valid @RequestBody CardIssueRequestDTO request,
            @RequestHeader("Authorization") String token) {
        log.info("Issue card request for customer: {}", request.getCustomerId());
        String jwt = token.substring(7); // Remove "Bearer " prefix
        CardResponseDTO response = cardService.issueCard(request, jwt);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Card issued successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
    public ResponseEntity<ApiResponse<List<CardListItemDTO>>> getAllCards(
            @RequestHeader("Authorization") String token) {
        log.info("Get all cards request");
        String jwt = token.substring(7);
        List<CardListItemDTO> cards = cardService.getAllCards(jwt);
        return ResponseEntity.ok(ApiResponse.success("Cards retrieved successfully", cards));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardResponseDTO>> getCardById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        log.info("Get card by ID request: {}", id);
        String jwt = token.substring(7);
        CardResponseDTO card = cardService.getCardById(id, jwt);
        return ResponseEntity.ok(ApiResponse.success("Card retrieved successfully", card));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<CardListItemDTO>>> getCardsByCustomerId(
            @PathVariable String customerId,
            @RequestHeader("Authorization") String token) {
        log.info("Get cards by customer ID request: {}", customerId);
        String jwt = token.substring(7);
        List<CardListItemDTO> cards = cardService.getCardsByCustomerId(customerId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Cards retrieved successfully", cards));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<CardListItemDTO>>> getCardsByAccountId(
            @PathVariable Long accountId,
            @RequestHeader("Authorization") String token) {
        log.info("Get cards by account ID request: {}", accountId);
        String jwt = token.substring(7);
        List<CardListItemDTO> cards = cardService.getCardsByAccountId(accountId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Cards retrieved successfully", cards));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
    public ResponseEntity<ApiResponse<List<CardListItemDTO>>> getCardsByStatus(
            @PathVariable String status,
            @RequestHeader("Authorization") String token) {
        log.info("Get cards by status request: {}", status);
        String jwt = token.substring(7);
        List<CardListItemDTO> cards = cardService.getCardsByStatus(status, jwt);
        return ResponseEntity.ok(ApiResponse.success("Cards retrieved successfully", cards));
    }

    @GetMapping("/expiring-soon")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
    public ResponseEntity<ApiResponse<List<CardListItemDTO>>> getCardsExpiringSoon(
            @RequestHeader("Authorization") String token) {
        log.info("Get cards expiring within 30 days request");
        String jwt = token.substring(7);
        List<CardListItemDTO> cards = cardService.getCardsExpiringWithin30Days(jwt);
        return ResponseEntity.ok(ApiResponse.success("Cards expiring soon retrieved successfully", cards));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CardResponseDTO>> updateCardStatus(
            @PathVariable Long id,
            @Valid @RequestBody CardStatusUpdateDTO request,
            @RequestHeader("Authorization") String token) {
        log.info("Update card status request for ID: {}", id);
        String jwt = token.substring(7);
        CardResponseDTO response = cardService.updateCardStatus(id, request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Card status updated successfully", response));
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<CardResponseDTO>> updateCardPin(
            @PathVariable Long id,
            @Valid @RequestBody CardPinUpdateDTO request,
            @RequestHeader("Authorization") String token) {
        log.info("Update card PIN request for ID: {}", id);
        String jwt = token.substring(7);
        CardResponseDTO response = cardService.updateCardPin(id, request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Card PIN updated successfully", response));
    }

    @PatchMapping("/{id}/limit")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
    public ResponseEntity<ApiResponse<CardResponseDTO>> updateCardLimit(
            @PathVariable Long id,
            @Valid @RequestBody CardLimitUpdateDTO request,
            @RequestHeader("Authorization") String token) {
        log.info("Update card limit request for ID: {}", id);
        String jwt = token.substring(7);
        CardResponseDTO response = cardService.updateCardLimit(id, request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Card limit updated successfully", response));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<CardResponseDTO>> activateCard(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        log.info("Activate card request for ID: {}", id);
        String jwt = token.substring(7);
        CardResponseDTO response = cardService.activateCard(id, jwt);
        return ResponseEntity.ok(ApiResponse.success("Card activated successfully", response));
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<ApiResponse<CardResponseDTO>> blockCard(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestHeader("Authorization") String token) {
        log.info("Block card request for ID: {}", id);
        String jwt = token.substring(7);
        CardResponseDTO response = cardService.blockCard(id, reason, jwt);
        return ResponseEntity.ok(ApiResponse.success("Card blocked successfully", response));
    }

    @PatchMapping("/{id}/unblock")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
    public ResponseEntity<ApiResponse<CardResponseDTO>> unblockCard(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        log.info("Unblock card request for ID: {}", id);
        String jwt = token.substring(7);
        CardResponseDTO response = cardService.unblockCard(id, jwt);
        return ResponseEntity.ok(ApiResponse.success("Card unblocked successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> cancelCard(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestHeader("Authorization") String token) {
        log.info("Cancel card request for ID: {}", id);
        String jwt = token.substring(7);
        cardService.cancelCard(id, reason, jwt);
        return ResponseEntity.ok(ApiResponse.success("Card cancelled successfully", null));
    }
}


//package com.izak.demoBankManagement.controller;
//
//import com.izak.demoBankManagement.dto.*;
//import com.izak.demoBankManagement.service.CardService;
//import com.izak.demoBankManagement.security.JwtUtil;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/cards")
//@RequiredArgsConstructor
//@Validated
//@Slf4j
//@CrossOrigin(origins = "http://localhost:4200")
//public class CardController {
//
//    private final CardService cardService;
//    private final JwtUtil jwtUtil;
//
//    @PostMapping
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
//    public ResponseEntity<ApiResponse<CardResponseDTO>> issueCard(
//            @Valid @RequestBody CardIssueRequestDTO request) {
//        log.info("Issue card request for customer: {}", request.getCustomerId());
//        CardResponseDTO response = cardService.issueCard(request);
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Card issued successfully", response));
//    }
//
//    @GetMapping
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
//    public ResponseEntity<ApiResponse<List<CardListItemDTO>>> getAllCards() {
//        log.info("Get all cards request");
//        List<CardListItemDTO> cards = cardService.getAllCards();
//        return ResponseEntity.ok(ApiResponse.success("Cards retrieved successfully", cards));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<CardResponseDTO>> getCardById(
//            @PathVariable Long id,
//            @RequestHeader("Authorization") String token) {
//        log.info("Get card by ID request: {}", id);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        CardResponseDTO card = cardService.getCardById(id);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role) && !card.getCustomerId().equals(customerId)) {
//            return ResponseEntity
//                    .status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.error("Access denied: You can only view your own cards"));
//        }
//
//        return ResponseEntity.ok(ApiResponse.success("Card retrieved successfully", card));
//    }
//
//    @GetMapping("/customer/{customerId}")
//    public ResponseEntity<ApiResponse<List<CardListItemDTO>>> getCardsByCustomerId(
//            @PathVariable String customerId,
//            @RequestHeader("Authorization") String token) {
//        log.info("Get cards by customer ID request: {}", customerId);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String tokenCustomerId = jwtUtil.extractCustomerId(jwt);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role) && !customerId.equals(tokenCustomerId)) {
//            return ResponseEntity
//                    .status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.error("Access denied: You can only view your own cards"));
//        }
//
//        List<CardListItemDTO> cards = cardService.getCardsByCustomerId(customerId);
//        return ResponseEntity.ok(ApiResponse.success("Cards retrieved successfully", cards));
//    }
//
//    @GetMapping("/account/{accountId}")
//    public ResponseEntity<ApiResponse<List<CardListItemDTO>>> getCardsByAccountId(
//            @PathVariable Long accountId,
//            @RequestHeader("Authorization") String token) {
//        log.info("Get cards by account ID request: {}", accountId);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        List<CardListItemDTO> cards = cardService.getCardsByAccountId(accountId);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role) && !cards.isEmpty()) {
//            String cardCustomerId = cards.get(0).getCustomerId();
//            if (!cardCustomerId.equals(customerId)) {
//                return ResponseEntity
//                        .status(HttpStatus.FORBIDDEN)
//                        .body(ApiResponse.error("Access denied: You can only view cards for your own accounts"));
//            }
//        }
//
//        return ResponseEntity.ok(ApiResponse.success("Cards retrieved successfully", cards));
//    }
//
//    @GetMapping("/status/{status}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
//    public ResponseEntity<ApiResponse<List<CardListItemDTO>>> getCardsByStatus(
//            @PathVariable String status) {
//        log.info("Get cards by status request: {}", status);
//        List<CardListItemDTO> cards = cardService.getCardsByStatus(status);
//        return ResponseEntity.ok(ApiResponse.success("Cards retrieved successfully", cards));
//    }
//
//    @GetMapping("/expiring-soon")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
//    public ResponseEntity<ApiResponse<List<CardListItemDTO>>> getCardsExpiringSoon() {
//        log.info("Get cards expiring within 30 days request");
//        List<CardListItemDTO> cards = cardService.getCardsExpiringWithin30Days();
//        return ResponseEntity.ok(ApiResponse.success("Cards expiring soon retrieved successfully", cards));
//    }
//
//    @PutMapping("/{id}/status")
//    public ResponseEntity<ApiResponse<CardResponseDTO>> updateCardStatus(
//            @PathVariable Long id,
//            @Valid @RequestBody CardStatusUpdateDTO request,
//            @RequestHeader("Authorization") String token) {
//        log.info("Update card status request for ID: {}", id);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        CardResponseDTO existingCard = cardService.getCardById(id);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role)) {
//            if (!existingCard.getCustomerId().equals(customerId)) {
//                return ResponseEntity
//                        .status(HttpStatus.FORBIDDEN)
//                        .body(ApiResponse.error("Access denied: You can only update your own cards"));
//            }
//            // Customers can only block their own cards, not activate or cancel
//            if (!"BLOCKED".equals(request.getStatus().toUpperCase())) {
//                return ResponseEntity
//                        .status(HttpStatus.FORBIDDEN)
//                        .body(ApiResponse.error("Customers can only block their cards. Contact support for other status changes."));
//            }
//        }
//
//        CardResponseDTO response = cardService.updateCardStatus(id, request);
//        return ResponseEntity.ok(ApiResponse.success("Card status updated successfully", response));
//    }
//
//    @PatchMapping("/{id}/pin")
//    public ResponseEntity<ApiResponse<CardResponseDTO>> updateCardPin(
//            @PathVariable Long id,
//            @Valid @RequestBody CardPinUpdateDTO request,
//            @RequestHeader("Authorization") String token) {
//        log.info("Update card PIN request for ID: {}", id);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        CardResponseDTO existingCard = cardService.getCardById(id);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role) && !existingCard.getCustomerId().equals(customerId)) {
//            return ResponseEntity
//                    .status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.error("Access denied: You can only update PIN for your own cards"));
//        }
//
//        CardResponseDTO response = cardService.updateCardPin(id, request);
//        return ResponseEntity.ok(ApiResponse.success("Card PIN updated successfully", response));
//    }
//
//    @PatchMapping("/{id}/limit")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
//    public ResponseEntity<ApiResponse<CardResponseDTO>> updateCardLimit(
//            @PathVariable Long id,
//            @Valid @RequestBody CardLimitUpdateDTO request) {
//        log.info("Update card limit request for ID: {}", id);
//        CardResponseDTO response = cardService.updateCardLimit(id, request);
//        return ResponseEntity.ok(ApiResponse.success("Card limit updated successfully", response));
//    }
//
//    @PatchMapping("/{id}/activate")
//    public ResponseEntity<ApiResponse<CardResponseDTO>> activateCard(
//            @PathVariable Long id,
//            @RequestHeader("Authorization") String token) {
//        log.info("Activate card request for ID: {}", id);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        CardResponseDTO existingCard = cardService.getCardById(id);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role) && !existingCard.getCustomerId().equals(customerId)) {
//            return ResponseEntity
//                    .status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.error("Access denied: You can only activate your own cards"));
//        }
//
//        CardResponseDTO response = cardService.activateCard(id);
//        return ResponseEntity.ok(ApiResponse.success("Card activated successfully", response));
//    }
//
//    @PatchMapping("/{id}/block")
//    public ResponseEntity<ApiResponse<CardResponseDTO>> blockCard(
//            @PathVariable Long id,
//            @RequestParam String reason,
//            @RequestHeader("Authorization") String token) {
//        log.info("Block card request for ID: {}", id);
//
//        String jwt = token.substring(7);
//        String role = jwtUtil.extractRole(jwt);
//        String customerId = jwtUtil.extractCustomerId(jwt);
//
//        CardResponseDTO existingCard = cardService.getCardById(id);
//
//        // Verify ownership for customers
//        if ("CUSTOMER".equals(role) && !existingCard.getCustomerId().equals(customerId)) {
//            return ResponseEntity
//                    .status(HttpStatus.FORBIDDEN)
//                    .body(ApiResponse.error("Access denied: You can only block your own cards"));
//        }
//
//        CardResponseDTO response = cardService.blockCard(id, reason);
//        return ResponseEntity.ok(ApiResponse.success("Card blocked successfully", response));
//    }
//
//    @PatchMapping("/{id}/unblock")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
//    public ResponseEntity<ApiResponse<CardResponseDTO>> unblockCard(@PathVariable Long id) {
//        log.info("Unblock card request for ID: {}", id);
//        CardResponseDTO response = cardService.unblockCard(id);
//        return ResponseEntity.ok(ApiResponse.success("Card unblocked successfully", response));
//    }
//
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'CARD_OFFICER')")
//    public ResponseEntity<ApiResponse<Void>> cancelCard(
//            @PathVariable Long id,
//            @RequestParam String reason) {
//        log.info("Cancel card request for ID: {}", id);
//        cardService.cancelCard(id, reason);
//        return ResponseEntity.ok(ApiResponse.success("Card cancelled successfully", null));
//    }
//}
//

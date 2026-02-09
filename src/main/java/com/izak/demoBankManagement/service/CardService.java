package com.izak.demoBankManagement.service;

import com.izak.demoBankManagement.dto.*;

import java.util.List;

public interface CardService {

    CardResponseDTO issueCard(CardIssueRequestDTO request, String jwtToken);

    CardResponseDTO getCardById(Long id, String jwtToken);

    CardResponseDTO getCardByCardNumber(String cardNumber, String jwtToken);

    List<CardListItemDTO> getAllCards(String jwtToken);

    List<CardListItemDTO> getCardsByCustomerId(String customerId, String jwtToken);

    List<CardListItemDTO> getCardsByAccountId(Long accountId, String jwtToken);

    List<CardListItemDTO> getCardsByStatus(String status, String jwtToken);

    List<CardListItemDTO> getCardsExpiringWithin30Days(String jwtToken);

    CardResponseDTO updateCardStatus(Long id, CardStatusUpdateDTO request, String jwtToken);

    CardResponseDTO updateCardPin(Long id, CardPinUpdateDTO request, String jwtToken);

    CardResponseDTO updateCardLimit(Long id, CardLimitUpdateDTO request, String jwtToken);

    CardResponseDTO activateCard(Long id, String jwtToken);

    CardResponseDTO blockCard(Long id, String reason, String jwtToken);

    CardResponseDTO unblockCard(Long id, String jwtToken);

    void cancelCard(Long id, String reason, String jwtToken);
}












//package com.izak.demoBankManagement.service;
//
//import com.izak.demoBankManagement.dto.*;
//
//import java.util.List;
//
//public interface CardService {
//
//    CardResponseDTO issueCard(CardIssueRequestDTO request);
//
//    CardResponseDTO getCardById(Long id);
//
//    CardResponseDTO getCardByCardNumber(String cardNumber);
//
//    List<CardListItemDTO> getAllCards();
//
//    List<CardListItemDTO> getCardsByCustomerId(String customerId);
//
//    List<CardListItemDTO> getCardsByAccountId(Long accountId);
//
//    List<CardListItemDTO> getCardsByStatus(String status);
//
//    List<CardListItemDTO> getCardsExpiringWithin30Days();
//
//    CardResponseDTO updateCardStatus(Long id, CardStatusUpdateDTO request);
//
//    CardResponseDTO updateCardPin(Long id, CardPinUpdateDTO request);
//
//    CardResponseDTO updateCardLimit(Long id, CardLimitUpdateDTO request);
//
//    CardResponseDTO activateCard(Long id);
//
//    CardResponseDTO blockCard(Long id, String reason);
//
//    CardResponseDTO unblockCard(Long id);
//
//    void cancelCard(Long id, String reason);
//}
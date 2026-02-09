package com.izak.demoBankManagement.repository;

import com.izak.demoBankManagement.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByCardNumber(String cardNumber);

    List<Card> findByCustomerId(Long customerId);

    List<Card> findByAccountId(Long accountId);

    List<Card> findByStatus(Card.Status status);

    List<Card> findByCardType(Card.CardType cardType);

    boolean existsByCardNumber(String cardNumber);

    @Query("SELECT c FROM Card c WHERE c.customer.customerId = :customerId")
    List<Card> findByCustomerCustomerId(@Param("customerId") String customerId);

    @Query("SELECT c FROM Card c WHERE c.account.accountNumber = :accountNumber")
    List<Card> findByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("SELECT c FROM Card c WHERE c.expiryDate BETWEEN :startDate AND :endDate AND c.status = 'ACTIVE'")
    List<Card> findCardsExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM Card c WHERE c.expiryDate <= :date AND c.status = 'ACTIVE'")
    List<Card> findExpiredActiveCards(@Param("date") LocalDate date);

    // Find cards expiring within 30 days - PostgreSQL uses CURRENT_DATE
    @Query("SELECT c FROM Card c WHERE c.expiryDate BETWEEN CURRENT_DATE AND :expiryDate AND c.status = 'ACTIVE'")
    List<Card> findCardsExpiringWithinDays(@Param("expiryDate") LocalDate expiryDate);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.customer.id = :customerId AND c.status = 'ACTIVE'")
    Long countActiveCardsByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.account.id = :accountId AND c.cardType = :cardType AND c.status = 'ACTIVE'")
    Long countActiveCardsByAccountAndType(@Param("accountId") Long accountId, @Param("cardType") Card.CardType cardType);

    // Branch-scoped query
    @Query("SELECT c FROM Card c WHERE c.account.branch.id = :branchId")
    List<Card> findByAccountBranchId(@Param("branchId") Long branchId);
}

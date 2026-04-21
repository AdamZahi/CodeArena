package com.codearena.module4_shop.repository;

import com.codearena.module4_shop.entity.Purchase;
import com.codearena.module4_shop.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    // Find all orders by participant
    List<Purchase> findByParticipantId(String participantId);

    // Find orders by status
    List<Purchase> findByStatus(OrderStatus status);

    // Find orders by participant and status
    List<Purchase> findByParticipantIdAndStatus(String participantId, OrderStatus status);

    // Total revenue
    @Query("SELECT SUM(p.totalPrice) FROM Purchase p WHERE p.status != 'CANCELLED'")
    Double calculateTotalRevenue();

    // Count orders by status
    Long countByStatus(OrderStatus status);

    @Query("SELECT COUNT(p) FROM Purchase p")
    Long countAll();

    // ── Find cancelled orders older than cutoff date (for cleanup cron) ──
    @Query("SELECT p FROM Purchase p WHERE p.status = 'CANCELLED' AND p.createdAt < :cutoffDate")
    List<Purchase> findCancelledOrdersBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
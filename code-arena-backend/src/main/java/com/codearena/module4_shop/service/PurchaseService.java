package com.codearena.module4_shop.service;

import com.codearena.module4_shop.dto.PurchaseRequest;
import com.codearena.module4_shop.dto.PurchaseResponse;
import com.codearena.module4_shop.enums.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface PurchaseService {

    // ── CRUD ─────────────────────────────────────

    // Checkout — create order from cart
    PurchaseResponse checkout(PurchaseRequest request);

    // Get all orders (Admin)
    List<PurchaseResponse> getAllOrders();

    // Get order by ID
    PurchaseResponse getOrderById(UUID id);

    // Get orders by participant
    List<PurchaseResponse> getOrdersByParticipant(String participantId);

    // Update order status (Admin)
    PurchaseResponse updateOrderStatus(UUID id, OrderStatus status);

    // Cancel order
    PurchaseResponse cancelOrder(UUID orderId, String participantId);

    // ── MÉTIERS AVANCÉS ───────────────────────────

    // Total revenue
    Double getTotalRevenue();

    // Count orders by status
    Long countByStatus(OrderStatus status);
    Long countAllOrders();

    // Best sellers
    List<Object[]> getBestSellers();
}
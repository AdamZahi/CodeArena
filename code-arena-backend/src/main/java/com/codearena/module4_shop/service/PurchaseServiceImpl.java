package com.codearena.module4_shop.service;

import com.codearena.module4_shop.dto.PurchaseItemRequest;
import com.codearena.module4_shop.dto.PurchaseRequest;
import com.codearena.module4_shop.dto.PurchaseResponse;
import com.codearena.module4_shop.entity.Purchase;
import com.codearena.module4_shop.entity.PurchaseItem;
import com.codearena.module4_shop.entity.ShopItem;
import com.codearena.module4_shop.enums.OrderStatus;
import com.codearena.module4_shop.repository.PurchaseItemRepository;
import com.codearena.module4_shop.repository.PurchaseRepository;
import com.codearena.module4_shop.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.codearena.user.repository.UserRepository;
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final ShopItemRepository shopItemRepository;
    private final ShopService shopService;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CouponService couponService;
    private final UserRepository userRepository;

    @Value("${app.shop.admin-email}")
    private String adminEmail;

    // ── CHECKOUT ─────────────────────────────────
    @Override
    @Transactional
    public PurchaseResponse checkout(PurchaseRequest request) {
        log.info("Processing checkout for participant: {}", request.getParticipantId());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Cart cannot be empty");
        }

        List<PurchaseItem> orderItems = new ArrayList<>();
        double totalPrice = 0.0;

        for (PurchaseItemRequest itemRequest : request.getItems()) {

            ShopItem product = shopItemRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found: " + itemRequest.getProductId()));

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for: " + product.getName() +
                                ". Available: " + product.getStock() +
                                ", Requested: " + itemRequest.getQuantity());
            }

            double discountedPrice = applyQuantityDiscount(product.getPrice(), itemRequest.getQuantity());
            totalPrice += discountedPrice * itemRequest.getQuantity();

            PurchaseItem item = PurchaseItem.builder()
                    .shopItem(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(applyQuantityDiscount(product.getPrice(), itemRequest.getQuantity())) // ← discounted
                    .build();

            orderItems.add(item);
        }

// ── APPLY COUPON BEFORE SAVING ────────────────
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            totalPrice = couponService.applyDiscount(totalPrice, request.getCouponCode());
            log.info("Coupon {} applied, new total: {}", request.getCouponCode(), totalPrice);
        }

        Purchase purchase = Purchase.builder()
                .participantId(request.getParticipantId())
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        Purchase savedPurchase = purchaseRepository.save(purchase);

        for (PurchaseItem item : orderItems) {
            item.setPurchase(savedPurchase);
            purchaseItemRepository.save(item);

            ShopItem product = item.getShopItem();
            product.setStock(product.getStock() - item.getQuantity());
            shopItemRepository.save(product);

// ── STOCK ALERT ───────────────────────────────
// Notify all clients when stock drops to low level
            if (product.getStock() <= 5 && product.getStock() > 0) {
                messagingTemplate.convertAndSend(
                        "/topic/stock-alert",
                        new java.util.HashMap<String, Object>() {{
                            put("productId", product.getId().toString());
                            put("productName", product.getName());
                            put("stock", product.getStock());
                            put("message", "⚠ Only " + product.getStock() + " left of " + product.getName() + "!");
                        }}
                );
            }}


        log.info("Order created successfully: {}", savedPurchase.getId());

        PurchaseResponse response = toResponse(savedPurchase, orderItems);

        try {
            // ── LOOK UP REAL EMAIL FROM USERS TABLE ───────
            String participantEmail = userRepository
                    .findByKeycloakId(request.getParticipantId())
                    .map(user -> user.getEmail())
                    .orElse(null);

            if (participantEmail != null) {
                emailService.sendOrderConfirmation(participantEmail, response);
                log.info("Confirmation email sent to: {}", participantEmail);
            } else {
                log.warn("No email found for participant: {}", request.getParticipantId());
            }
            emailService.sendAdminOrderAlert(adminEmail, response);
        } catch (Exception e) {
            log.warn("Email failed but order placed: {}", e.getMessage());
        }

        return response;
    }

    // ── CRUD ─────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseResponse> getAllOrders() {
        return purchaseRepository.findAll()
                .stream()
                .map(p -> toResponse(p, p.getItems()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseResponse getOrderById(UUID id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        return toResponse(purchase, purchase.getItems());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseResponse> getOrdersByParticipant(String participantId) {
        return purchaseRepository.findByParticipantId(participantId)
                .stream()
                .map(p -> toResponse(p, p.getItems()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PurchaseResponse updateOrderStatus(UUID id, OrderStatus status) {
        log.info("Updating order {} status to {}", id, status);
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        purchase.setStatus(status);
        PurchaseResponse response = toResponse(purchaseRepository.save(purchase), purchase.getItems());

        // ── WEBSOCKET NOTIFICATION ─────────────────
        // Push real-time notification to the specific participant
        messagingTemplate.convertAndSend(
                "/topic/orders/" + purchase.getParticipantId(),
                new java.util.HashMap<String, String>() {{
                    put("orderId", purchase.getId().toString().substring(0, 8).toUpperCase());
                    put("status", status.name());
                    put("message", buildStatusMessage(status));
                }}
        );

        return response;
    }

    @Override
    @Transactional
    public PurchaseResponse cancelOrder(UUID id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        if (purchase.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only PENDING orders can be cancelled");
        }

        for (PurchaseItem item : purchase.getItems()) {
            ShopItem product = item.getShopItem();
            product.setStock(product.getStock() + item.getQuantity());
            shopItemRepository.save(product);
        }

        purchase.setStatus(OrderStatus.CANCELLED);
        return toResponse(purchaseRepository.save(purchase), purchase.getItems());
    }

    // ── METIERS AVANCES ───────────────────────────

    @Override
    public Double getTotalRevenue() {
        Double revenue = purchaseRepository.calculateTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }

    @Override
    public Long countByStatus(OrderStatus status) {
        if (status == null) return purchaseRepository.count();
        return purchaseRepository.countByStatus(status);
    }

    @Override
    public Long countAllOrders() {
        return purchaseRepository.count();
    }

    @Override
    public List<Object[]> getBestSellers() {
        return purchaseItemRepository.findBestSellers();
    }

    // ── HELPER ────────────────────────────────────
    private PurchaseResponse toResponse(Purchase purchase, List<PurchaseItem> items) {
        List<PurchaseResponse.PurchaseItemResponse> itemResponses = items.stream()
                .map((PurchaseItem item) -> PurchaseResponse.PurchaseItemResponse.builder()
                        .id(item.getId())
                        .product(shopService.getProductById(item.getShopItem().getId()))
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return PurchaseResponse.builder()
                .id(purchase.getId())
                .participantId(purchase.getParticipantId())
                .totalPrice(purchase.getTotalPrice())
                .status(purchase.getStatus())
                .createdAt(purchase.getCreatedAt())
                .items(itemResponses)
                .build();
    }
    // ── WEBSOCKET HELPER ──────────────────────────
    private String buildStatusMessage(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "Your order has been confirmed! 🎉";
            case SHIPPED   -> "Your order is on its way! 🚚";
            case DELIVERED -> "Your order has been delivered! ✅";
            case CANCELLED -> "Your order has been cancelled. ❌";
            default        -> "Your order status has been updated.";
        };
    }
    // ── DISCOUNT ALGORITHM ────────────────────────
    // Quantity-based tiered discount
    private double applyQuantityDiscount(double unitPrice, int quantity) {
        if (quantity >= 5) {
            return unitPrice * 0.80; // 20% off
        } else if (quantity >= 3) {
            return unitPrice * 0.90; // 10% off
        } else if (quantity >= 2) {
            return unitPrice * 0.95; // 5% off
        }
        return unitPrice; // no discount
    }

}

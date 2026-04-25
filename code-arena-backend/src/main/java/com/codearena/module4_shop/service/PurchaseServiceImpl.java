package com.codearena.module4_shop.service;

import com.codearena.module4_shop.dto.PurchaseItemRequest;
import com.codearena.module4_shop.dto.PurchaseRequest;
import com.codearena.module4_shop.dto.PurchaseResponse;
import com.codearena.module4_shop.entity.Purchase;
import com.codearena.module4_shop.entity.PurchaseItem;
import com.codearena.module4_shop.entity.ShopItem;
import com.codearena.module4_shop.enums.OrderStatus;
import com.codearena.module4_shop.exception.CartEmptyException;
import com.codearena.module4_shop.exception.InsufficientStockException;
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
// @Service tells Spring this is a service bean — can be injected anywhere
@RequiredArgsConstructor
// @RequiredArgsConstructor generates constructor for all final fields
// Spring sees the constructor and injects all dependencies automatically
public class PurchaseServiceImpl implements PurchaseService {
    // implements PurchaseService (interface) — controller depends on interface
    // not on this implementation — Dependency Inversion Principle

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final ShopItemRepository shopItemRepository;
    private final ShopService shopService;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    // SimpMessagingTemplate — Spring's WebSocket sender
    // used to broadcast messages to specific STOMP topics
    private final CouponService couponService;
    private final UserRepository userRepository;
    // UserRepository from user module — used to look up participant's email
    // by their Auth0 keycloakId to send confirmation email
    private final LoyaltyService loyaltyService;

    @Value("${app.shop.admin-email}")
    // @Value reads from application.yml
    // app.shop.admin-email: islemazzouz04@gmail.com
    private String adminEmail;

    // ── CHECKOUT ─────────────────────────────────
    // @Transactional = ALL steps succeed or ALL rollback
    // If step 5 (save order) fails after step 2 (deduct stock) succeeded
    // → everything rolls back → no data corruption
    @Override
    @Transactional
    public PurchaseResponse checkout(PurchaseRequest request) {
        // SECURITY NOTE: participantId here was already overridden
        // by PurchaseController using jwt.getSubject()
        // So we can trust it — it came from the signed JWT token
        log.info("Processing checkout for participant: {}", request.getParticipantId());

        // ── STEP 1: VALIDATE CART NOT EMPTY ──────
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new CartEmptyException();            // @Transactional rolls back everything if any exception is thrown
        }

        List<PurchaseItem> orderItems = new ArrayList<>();
        double totalPrice = 0.0;

        // ── STEP 2: VALIDATE STOCK + CALCULATE TOTAL ──
        // We validate ALL items BEFORE touching any stock
        // This is important — if item 3 fails we don't want
        // to have already deducted stock for items 1 and 2
        for (PurchaseItemRequest itemRequest : request.getItems()) {

            ShopItem product = shopItemRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found: " + itemRequest.getProductId()));
            // orElseThrow — if product doesn't exist → throw exception
            // @Transactional catches it → rolls back everything

            // ── STOCK CHECK ───────────────────────
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                        product.getName(),
                        product.getStock(),
                        itemRequest.getQuantity());
                // Clear error message tells frontend exactly what's wrong
            }

            // ── APPLY QUANTITY DISCOUNT ───────────
            // applyQuantityDiscount() applies tiered discount:
            // qty >= 5 → 20% off, qty >= 3 → 10% off, qty >= 2 → 5% off
            double discountedPrice = applyQuantityDiscount(
                    product.getPrice(), itemRequest.getQuantity()
            );
            totalPrice += discountedPrice * itemRequest.getQuantity();

            // Build PurchaseItem — one line in the order
            PurchaseItem item = PurchaseItem.builder()
                    .shopItem(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(discountedPrice)
                    // unitPrice stores the discounted price at time of purchase
                    // WHY? Product price can change later (dynamic pricing)
                    // Storing unitPrice preserves historical price — important
                    // for receipts, refunds, and financial records
                    .build();

            orderItems.add(item);
        }

        // ── STEP 3: APPLY COUPON ──────────────────
        // Applied AFTER quantity discounts
        // Both discounts can stack — quantity first, then coupon
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            totalPrice = couponService.applyDiscount(totalPrice, request.getCouponCode());
            log.info("Coupon {} applied, new total: {}", request.getCouponCode(), totalPrice);
        }

        // ── STEP 4: SAVE ORDER TO DATABASE ───────
        Purchase purchase = Purchase.builder()
                .participantId(request.getParticipantId())
                // participantId = Auth0 sub from JWT (set in controller)
                // Links this order to the logged-in user
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                // New orders start as PENDING
                // Admin changes to CONFIRMED → SHIPPED → DELIVERED
                .items(new ArrayList<>())
                .build();

        Purchase savedPurchase = purchaseRepository.save(purchase);
        // save() on new entity → INSERT INTO purchases
        // returns saved entity with generated UUID id

        // ── STEP 5: SAVE ITEMS + DEDUCT STOCK ────
        for (PurchaseItem item : orderItems) {
            item.setPurchase(savedPurchase);
            // Link each PurchaseItem to the parent Purchase
            purchaseItemRepository.save(item);
            // INSERT INTO purchase_items

            // ── DEDUCT STOCK ──────────────────────
            ShopItem product = item.getShopItem();
            product.setStock(product.getStock() - item.getQuantity());
            shopItemRepository.save(product);
            // UPDATE shop_items SET stock = stock - quantity WHERE id = ?

            // ── STOCK ALERT WEBSOCKET ─────────────
            // If stock drops to 5 or below after purchase
            // broadcast warning to ALL connected users
            if (product.getStock() <= 5 && product.getStock() > 0) {
                messagingTemplate.convertAndSend(
                        "/topic/stock-alert",
                        // /topic/stock-alert = public topic
                        // ALL connected users receive this — no role check needed
                        // because knowing a product is low stock is public info
                        new java.util.HashMap<String, Object>() {{
                            put("productId",   product.getId().toString());
                            put("productName", product.getName());
                            put("stock",       product.getStock());
                            put("message",
                                    "⚠ Only " + product.getStock() +
                                            " left of " + product.getName() + "!");
                        }}
                );
            }
        }

        log.info("Order created successfully: {}", savedPurchase.getId());

        // ── STEP 6: ADMIN WEBSOCKET NOTIFICATION ──
        // Notify admin in real time when a new order is placed
        // Goes to /topic/admin/new-order
        // Frontend NotificationComponent checks role === 'ADMIN'
        // before showing the toast — so only admin sees it
        messagingTemplate.convertAndSend(
                "/topic/admin/new-order",
                new java.util.HashMap<String, Object>() {{
                    put("orderId",
                            savedPurchase.getId().toString()
                                    .substring(0, 8).toUpperCase());
                    // First 8 chars of UUID for readability: "550E8400"
                    put("participantId", savedPurchase.getParticipantId());
                    put("total",         savedPurchase.getTotalPrice());
                    put("message",
                            "New order placed! $" +
                                    String.format("%.2f", savedPurchase.getTotalPrice()));
                }}
        );

        PurchaseResponse response = toResponse(savedPurchase, orderItems);

        // ── STEP 7: SEND EMAILS ───────────────────
        // Wrapped in try-catch so email failure does NOT rollback the order
        // @Transactional only rolls back on uncaught exceptions
        // We catch here → order is saved regardless of email success
        try {
            // Look up participant's real email from our users table
            // using their Auth0 keycloakId (same as participantId)
            String participantEmail = userRepository
                    .findByAuth0Id(request.getParticipantId())
                    .map(user -> user.getEmail())
                    // .map() transforms Optional<User> to Optional<String>
                    .orElse(null);
            // if user not found → null (no email sent)

            if (participantEmail != null) {
                emailService.sendOrderConfirmation(participantEmail, response);
                // Sends confirmation email to participant via Gmail SMTP
                log.info("Confirmation email sent to: {}", participantEmail);
            } else {
                log.warn("No email found for participant: {}",
                        request.getParticipantId());
                // This happens when UserSyncFilter hasn't synced the user yet
                // or when testing without Auth0
            }

            emailService.sendAdminOrderAlert(adminEmail, response);
            // Always send admin alert — adminEmail from application.yml
        } catch (Exception e) {
            log.warn("Email failed but order placed: {}", e.getMessage());
            // Email failure = warning log only, NOT an error
            // Order is already committed at this point
        }

// ── STEP 8: EARN LOYALTY POINTS + ECO BONUS ──
// Standard: 1 point per $1 spent
// Eco bonus: extra points for buying sustainable products (SDG 12)
        try {
            int earned = loyaltyService.earnPoints(
                    request.getParticipantId(), totalPrice
            );
            log.info("Participant earned {} loyalty points", earned);

            // ── ECO BONUS: reward sustainable purchases ──
            // Calculate average eco score across all ordered items
            // Items with ecoScore stored in DB get bonus points
            int totalEcoScore = 0;
            int scoredItems = 0;
            for (PurchaseItem item : orderItems) {
                Integer ecoScore = item.getShopItem().getEcoScore();
                if (ecoScore != null) {
                    totalEcoScore += ecoScore;
                    scoredItems++;
                }
            }
            if (scoredItems > 0) {
                int avgEcoScore = totalEcoScore / scoredItems;
                int ecoBonus = loyaltyService.earnEcoBonus(
                        request.getParticipantId(), avgEcoScore
                );
                if (ecoBonus > 0) {
                    log.info("🌱 Eco bonus: +{} points for avg ECO {}/10",
                            ecoBonus, avgEcoScore);
                }
            }
        } catch (Exception e) {
            log.warn("Loyalty points failed but order placed: {}", e.getMessage());
        }

        return response;
        // Returns the complete order response to controller
        // Controller returns it to Angular with 201 Created status
    }

    // ── GET ALL ORDERS (Admin) ────────────────────
    @Override
    @Transactional(readOnly = true)
    // readOnly = true → Hibernate knows not to track changes
    // → better performance for read-only operations
    public List<PurchaseResponse> getAllOrders() {
        return purchaseRepository.findAll()
                // SELECT * FROM purchases
                .stream()
                .map(p -> toResponse(p, p.getItems()))
                // convert each Purchase entity → PurchaseResponse DTO
                .collect(Collectors.toList());
    }

    // ── GET ORDER BY ID ───────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PurchaseResponse getOrderById(UUID id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        // 404-style error — caught by @ExceptionHandler → 400 response
        return toResponse(purchase, purchase.getItems());
    }

    // ── GET ORDERS BY PARTICIPANT ─────────────────
    // Called by getMyOrders() in controller
    // participantId already verified as JWT subject — cannot be spoofed
    @Override
    @Transactional(readOnly = true)
    public List<PurchaseResponse> getOrdersByParticipant(String participantId) {
        return purchaseRepository.findByParticipantId(participantId)
                // Spring Data JPA derives: SELECT * FROM purchases
                // WHERE participant_id = ?
                .stream()
                .map(p -> toResponse(p, p.getItems()))
                .collect(Collectors.toList());
    }

    // ── UPDATE ORDER STATUS (Admin) ───────────────
    @Override
    @Transactional
    public PurchaseResponse updateOrderStatus(UUID id, OrderStatus status) {
        log.info("Updating order {} status to {}", id, status);
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        purchase.setStatus(status);
        PurchaseResponse response = toResponse(
                purchaseRepository.save(purchase), purchase.getItems()
        );

        // ── WEBSOCKET NOTIFICATION TO PARTICIPANT ──
        // Push real-time notification to the SPECIFIC participant
        // /topic/orders/{participantId} = private topic per user
        // Only the participant whose order changed receives this
        // They see a toast: "Your order is on its way! 🚚"
        messagingTemplate.convertAndSend(
                "/topic/orders/" + purchase.getParticipantId(),
                new java.util.HashMap<String, String>() {{
                    put("orderId",
                            purchase.getId().toString()
                                    .substring(0, 8).toUpperCase());
                    put("status",  status.name());
                    put("message", buildStatusMessage(status));
                    // buildStatusMessage returns human-readable text
                    // with emoji based on status
                }}
        );

        return response;
    }

    // ── CANCEL ORDER ──────────────────────────────
    // SECURITY FIX: Added participantId parameter
    // Controller extracts participantId from JWT and passes it here
    // Service verifies the order belongs to this participant
    // before allowing cancellation
    @Override
    @Transactional
    public PurchaseResponse cancelOrder(UUID id, String participantId) {
        // participantId = jwt.getSubject() from controller
        // Cannot be faked — comes from signed JWT token

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        // ── OWNERSHIP CHECK ───────────────────────
        // SECURITY: Verify this order belongs to the requesting participant
        // Without this check: user A could cancel user B's order
        // by calling PUT /api/shop/orders/{B's order ID}/cancel
        if (!purchase.getParticipantId().equals(participantId)) {
            throw new RuntimeException(
                    "Access denied — this is not your order"
            );
            // In production: throw AccessDeniedException → 403 Forbidden
            // Currently: caught by @ExceptionHandler → 400 Bad Request
        }

        // ── STATUS CHECK ──────────────────────────
        // Only PENDING orders can be cancelled
        // CONFIRMED, SHIPPED, DELIVERED orders cannot be cancelled
        // (admin has already started processing them)
        if (purchase.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException(
                    "Only PENDING orders can be cancelled. Current status: "
                            + purchase.getStatus()
            );
        }

        // ── RESTORE STOCK ─────────────────────────
        // Give back stock for every cancelled item
        // This is why @Transactional is important here:
        // If restoring stock for item 3 fails, we don't want
        // items 1 and 2 to have their stock restored while item 3 wasn't
        // → @Transactional rolls back all stock changes on failure
        for (PurchaseItem item : purchase.getItems()) {
            ShopItem product = item.getShopItem();
            product.setStock(product.getStock() + item.getQuantity());
            // Add back the cancelled quantity to available stock
            shopItemRepository.save(product);
            log.info("Restored {} units of {} to stock",
                    item.getQuantity(), product.getName());
        }

        purchase.setStatus(OrderStatus.CANCELLED);
        Purchase saved = purchaseRepository.save(purchase);

// ── DEDUCT LOYALTY POINTS ─────────────────────
// Remove points earned from this order when cancelled
// Prevents exploit: place order → earn points → cancel → keep points
        try {
            int pointsToDeduct = purchase.getTotalPrice() != null
    ? (int) Math.floor(purchase.getTotalPrice())
    : 0;
            loyaltyService.deductPoints(participantId, pointsToDeduct);
            log.info("Deducted {} loyalty points for cancelled order {}",
                    pointsToDeduct, id.toString().substring(0, 8).toUpperCase());
        } catch (Exception e) {
            log.warn("Point deduction failed but order cancelled: {}", e.getMessage());
        }

        return toResponse(saved, purchase.getItems());
    }

    // ── MÉTIERS AVANCÉS ───────────────────────────

    @Override
    public Double getTotalRevenue() {
        Double revenue = purchaseRepository.calculateTotalRevenue();
        // JPQL query in PurchaseRepository:
        // @Query("SELECT SUM(p.totalPrice) FROM Purchase p
        //         WHERE p.status != 'CANCELLED'")
        return revenue != null ? revenue : 0.0;
        // null check — if no orders exist, SUM returns null
        // we return 0.0 instead to avoid NullPointerException
    }

    @Override
    public Long countByStatus(OrderStatus status) {
        if (status == null) return purchaseRepository.count();
        // null = count ALL orders regardless of status
        return purchaseRepository.countByStatus(status);
        // Spring Data JPA derives:
        // SELECT COUNT(*) FROM purchases WHERE status = ?
    }

    @Override
    public Long countAllOrders() {
        return purchaseRepository.count();
        // SELECT COUNT(*) FROM purchases
    }

    @Override
    public List<Object[]> getBestSellers() {
        return purchaseItemRepository.findBestSellers();
        // JPQL: SELECT shopItem, SUM(quantity) FROM PurchaseItem
        //       GROUP BY shopItem ORDER BY SUM(quantity) DESC
        // Returns Object[] because result has 2 columns: product + count
    }

    // ── HELPER — Entity to DTO ────────────────────
    // Converts Purchase entity to PurchaseResponse DTO
    // WHY DTO? Never expose entity directly — entity has JPA annotations,
    // lazy-loaded relations, and internal fields we don't want in the API
    private PurchaseResponse toResponse(Purchase purchase, List<PurchaseItem> items) {
        List<PurchaseResponse.PurchaseItemResponse> itemResponses = items.stream()
                .map((PurchaseItem item) ->
                        PurchaseResponse.PurchaseItemResponse.builder()
                                .id(item.getId())
                                .product(shopService.getProductById(item.getShopItem().getId()))
                                // Look up full product details for the response
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                // unitPrice = price at time of purchase (historical)
                                .build()
                )
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

    // ── WEBSOCKET STATUS MESSAGE HELPER ───────────
    // Returns human-readable message based on order status
    // Used in updateOrderStatus WebSocket notification
    private String buildStatusMessage(OrderStatus status) {
        return switch (status) {
            // Java 14+ switch expression — cleaner than if-else chain
            case CONFIRMED -> "Your order has been confirmed! 🎉";
            case SHIPPED   -> "Your order is on its way! 🚚";
            case DELIVERED -> "Your order has been delivered! ✅";
            case CANCELLED -> "Your order has been cancelled. ❌";
            default        -> "Your order status has been updated.";
        };
    }

    // ── DISCOUNT ALGORITHM ────────────────────────
    // Quantity-based tiered discount applied per item
    // Encourages bulk buying — standard e-commerce volume discount
    // discountedPrice = originalPrice × (1 - discountRate)
    private double applyQuantityDiscount(double unitPrice, int quantity) {
        if (quantity >= 5) {
            return unitPrice * 0.80;
            // 20% off — bulk buyer: buy 5+ get 20% off each
        } else if (quantity >= 3) {
            return unitPrice * 0.90;
            // 10% off — regular buyer: buy 3+ get 10% off each
        } else if (quantity >= 2) {
            return unitPrice * 0.95;
            // 5% off — starter: buy 2 get 5% off each
        }
        return unitPrice;
        // no discount for single items
    }
}
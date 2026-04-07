package com.codearena.module4_shop.controller;

import com.codearena.module4_shop.dto.ApiResponse;
import com.codearena.module4_shop.dto.PurchaseRequest;
import com.codearena.module4_shop.dto.PurchaseResponse;
import com.codearena.module4_shop.enums.OrderStatus;
import com.codearena.module4_shop.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Slf4j
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final QrCodeService qrCodeService;
    private final ExcelService excelService;
    private final CouponService couponService;
    private final LoyaltyService loyaltyService;
    private final StripeService stripeService;

    // ── CHECKOUT ─────────────────────────────────
    // POST /api/shop/orders/checkout
    // FIX 1: Override participantId from JWT — client cannot spoof identity
    // Any authenticated user can checkout — but only for themselves
    @PostMapping("/orders/checkout")
    public ResponseEntity<ApiResponse<PurchaseResponse>> checkout(
            @Valid @RequestBody PurchaseRequest request,
            @AuthenticationPrincipal Jwt jwt
            // @AuthenticationPrincipal Jwt jwt → Spring injects the JWT token
            // of the currently logged-in user — cannot be faked
    ) {
        // CRITICAL: ignore whatever participantId the client sent
        // always use the identity from the signed JWT token
        request.setParticipantId(jwt.getSubject());
        // jwt.getSubject() = Auth0 sub e.g. "google-oauth2|108378..."
        // This is cryptographically signed — impossible to forge

        PurchaseResponse response = purchaseService.checkout(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Order placed successfully"));
    }

    // ── GET ALL ORDERS (Admin ONLY) ──────────────
    // GET /api/shop/orders
    // FIX 5: Only ADMIN can see all orders
    @PreAuthorize("hasRole('ADMIN')")
    // hasRole('ADMIN') checks Spring Security authority "ROLE_ADMIN"
    // set by JwtAuthConverter which reads from our DB
    // if user is not ADMIN → 403 Forbidden automatically
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getAllOrders() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.getAllOrders(),
                        "Orders fetched successfully"
                )
        );
    }

    // ── GET ORDER BY ID (Admin ONLY) ─────────────
    // GET /api/shop/orders/{id}
    // FIX 5: Only ADMIN can view any specific order by ID
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<PurchaseResponse>> getOrderById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.getOrderById(id),
                        "Order fetched successfully"
                )
        );
    }

    // ── GET MY ORDERS (Participant) ──────────────
    // GET /api/shop/orders/me
    // FIX 2: Replaced /orders/my/{participantId} with /orders/me
    // participantId now comes from JWT — not from URL
    // BEFORE: GET /orders/my/google-oauth2|victim → anyone could change this!
    // AFTER:  GET /orders/me → backend reads identity from JWT
    @GetMapping("/orders/me")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String participantId = jwt.getSubject();
        // jwt.getSubject() is the Auth0 sub of the LOGGED IN user
        // cannot be manipulated by the client — it's in the signed token

        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.getOrdersByParticipant(participantId),
                        "Your orders fetched"
                )
        );
    }

    // ── UPDATE ORDER STATUS (Admin ONLY) ─────────
    // PUT /api/shop/orders/{id}/status
    // FIX 5: Only ADMIN can change order status
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<PurchaseResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.updateOrderStatus(id, status),
                        "Order status updated to " + status
                )
        );
    }

    // ── CANCEL ORDER ─────────────────────────────
    // PUT /api/shop/orders/{id}/cancel
    // Participant can cancel their OWN order only
    // We verify ownership in the service layer
    @PutMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<PurchaseResponse>> cancelOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String participantId = jwt.getSubject();
        // Pass participantId to service so it can verify
        // the order belongs to this user before cancelling

        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.cancelOrder(id, participantId),
                        // cancelOrder now takes participantId to verify ownership
                        "Order cancelled successfully"
                )
        );
    }

    // ── BEST SELLERS (Admin ONLY) ─────────────────
    // GET /api/shop/orders/best-sellers
    // FIX 5: Analytics — admin only
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders/best-sellers")
    public ResponseEntity<ApiResponse<List<Object[]>>> getBestSellers() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.getBestSellers(),
                        "Best sellers fetched"
                )
        );
    }

    // ── REVENUE STATS (Admin ONLY) ────────────────
    // GET /api/shop/orders/revenue
    // FIX 5: Revenue stats — admin only
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders/revenue")
    public ResponseEntity<ApiResponse<Double>> getTotalRevenue() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.getTotalRevenue(),
                        "Revenue calculated"
                )
        );
    }

    // ── QR CODE (Admin ONLY) ──────────────────────
    // GET /api/shop/orders/{id}/qr
    // FIX 5: QR generation — admin only
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders/{id}/qr")
    public ResponseEntity<ApiResponse<String>> getOrderQr(@PathVariable UUID id) {
        PurchaseResponse order = purchaseService.getOrderById(id);
        String qr = qrCodeService.generateOrderQr(
                order.getId().toString(),
                order.getParticipantId(),
                order.getTotalPrice()
        );
        return ResponseEntity.ok(ApiResponse.success(qr, "QR generated"));
    }

    // ── EXPORT ORDERS (Admin ONLY) ────────────────
    // GET /api/shop/orders/export
    // FIX 5: Excel export — admin only
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportOrders() throws Exception {
        List<PurchaseResponse> orders = purchaseService.getAllOrders();
        byte[] excel = excelService.exportOrders(orders);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=orders.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excel);
    }

    // ── VALIDATE COUPON ───────────────────────────
    // POST /api/shop/coupons/validate
    // Any authenticated user can validate a coupon
    // No role restriction needed — participants use coupons
    @PostMapping("/coupons/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateCoupon(
            @RequestBody Map<String, String> body
    ) {
        String code = body.get("code");
        boolean valid = couponService.isValid(code);

        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);
        result.put("code", code != null ? code.toUpperCase() : "");
        result.put("discountRate", valid ? couponService.getDiscountRate(code) : 0);
        result.put("message", valid
                ? "Coupon applied! " + (int)(couponService.getDiscountRate(code) * 100) + "% off"
                : "Invalid coupon code");

        return ResponseEntity.ok(ApiResponse.success(result, "Coupon checked"));
    }

    // ── GET MY LOYALTY POINTS ─────────────────────
    // GET /api/shop/loyalty/me
    // FIX 3: Replaced /loyalty/{participantId} with /loyalty/me
    // BEFORE: GET /loyalty/victim-id → anyone could view anyone's points!
    // AFTER:  GET /loyalty/me → backend reads identity from JWT
    @GetMapping("/loyalty/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLoyaltyPoints(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String participantId = jwt.getSubject();
        // always reads the logged-in user's points
        // impossible to read someone else's points

        int points = loyaltyService.getPoints(participantId);
        double redeemableValue = loyaltyService.getRedeemableValue(participantId);

        Map<String, Object> result = new HashMap<>();
        result.put("points", points);
        result.put("redeemableValue", redeemableValue);
        result.put("canRedeem", loyaltyService.canRedeem(participantId));

        return ResponseEntity.ok(ApiResponse.success(result, "Points fetched"));
    }

    // ── REDEEM POINTS ─────────────────────────────
    // POST /api/shop/loyalty/redeem
    // FIX 4: CRITICAL — replaced body participantId with JWT
    // BEFORE: body had { "participantId": "victim-id", "points": 100 }
    //         attacker could redeem ANYONE's points!
    // AFTER:  participantId comes from JWT — only redeem YOUR OWN points
    @PostMapping("/loyalty/redeem")
    public ResponseEntity<ApiResponse<Map<String, Object>>> redeemPoints(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt
            // JWT is injected by Spring Security — cannot be manipulated
    ) {
        String participantId = jwt.getSubject();
        // CRITICAL: we ignore body.get("participantId") completely
        // even if attacker sends someone else's ID → we use JWT subject

        int pointsToRedeem = (Integer) body.get("points");
        // only "points" is read from body — participantId is ignored

        double discount = loyaltyService.redeemPoints(participantId, pointsToRedeem);

        Map<String, Object> result = new HashMap<>();
        result.put("discount", discount);
        result.put("remainingPoints", loyaltyService.getPoints(participantId));

        return ResponseEntity.ok(ApiResponse.success(result, "Points redeemed successfully"));
    }

    // ── CREATE PAYMENT INTENT ─────────────────────
    // POST /api/shop/payment/create-intent
    // Any authenticated user can create a payment intent
    // Secret key stays on backend — publishable key goes to frontend
    @PostMapping("/payment/create-intent")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPaymentIntent(
            @RequestBody Map<String, Object> body
    ) {
        try {
            double amount = Double.parseDouble(body.get("amount").toString());
            String currency = body.getOrDefault("currency", "usd").toString();
            Map<String, String> result = stripeService.createPaymentIntent(amount, currency);
            return ResponseEntity.ok(ApiResponse.success(result, "Payment intent created"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create payment intent: " + e.getMessage()));
        }
    }

    // ── GET PUBLISHABLE KEY ───────────────────────
    // GET /api/shop/payment/config
    // Safe to expose — publishable key is meant to be public
    @GetMapping("/payment/config")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPaymentConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("publishableKey", stripeService.getPublishableKey());
        return ResponseEntity.ok(ApiResponse.success(config, "Payment config"));
    }

    // ── EXCEPTION HANDLER ─────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Purchase error: {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }
}
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
    // Main métier avancé — creates order + decrements stock
    @PostMapping("/orders/checkout")
    public ResponseEntity<ApiResponse<PurchaseResponse>> checkout(
            @Valid @RequestBody PurchaseRequest request
    ) {
        PurchaseResponse response = purchaseService.checkout(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Order placed successfully"));
    }

    // ── GET ALL ORDERS (Admin) ───────────────────
    // GET /api/shop/orders
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getAllOrders() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.getAllOrders(),
                        "Orders fetched successfully"
                )
        );
    }

    // ── GET ORDER BY ID ──────────────────────────
    // GET /api/shop/orders/{id}
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
    // GET /api/shop/orders/my/{participantId}
    @GetMapping("/orders/my/{participantId}")
    public ResponseEntity<ApiResponse<List<PurchaseResponse>>> getMyOrders(
            @PathVariable String participantId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.getOrdersByParticipant(participantId),
                        "Your orders fetched"
                )
        );
    }

    // ── UPDATE ORDER STATUS (Admin) ──────────────
    // PUT /api/shop/orders/{id}/status
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
    @PutMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<PurchaseResponse>> cancelOrder(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.cancelOrder(id),
                        "Order cancelled successfully"
                )
        );
    }

    // ── BEST SELLERS (Métier Avancé) ─────────────
    // GET /api/shop/orders/best-sellers
    @GetMapping("/orders/best-sellers")
    public ResponseEntity<ApiResponse<List<Object[]>>> getBestSellers() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.getBestSellers(),
                        "Best sellers fetched"
                )
        );
    }

    // ── REVENUE STATS (Métier Avancé) ────────────
    // GET /api/shop/orders/revenue
    @GetMapping("/orders/revenue")
    public ResponseEntity<ApiResponse<Double>> getTotalRevenue() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        purchaseService.getTotalRevenue(),
                        "Revenue calculated"
                )
        );
    }

    // ── EXCEPTION HANDLER ────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Purchase error: {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }
    // Add new endpoint:
// GET /api/shop/orders/{id}/qr
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
    // GET /api/shop/export/orders
    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportOrders() throws Exception {
        List<PurchaseResponse> orders = purchaseService.getAllOrders();
        byte[] excel = excelService.exportOrders(orders);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=orders.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excel);
    }
    // ── VALIDATE COUPON ──────────────────────────
// POST /api/shop/coupons/validate
    @PostMapping("/coupons/validate")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> validateCoupon(
            @RequestBody java.util.Map<String, String> body
    ) {
        String code = body.get("code");
        boolean valid = couponService.isValid(code);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("valid", valid);
        result.put("code", code != null ? code.toUpperCase() : "");
        result.put("discountRate", valid ? couponService.getDiscountRate(code) : 0);
        result.put("message", valid
                ? "Coupon applied! " + (int)(couponService.getDiscountRate(code) * 100) + "% off"
                : "Invalid coupon code");

        return ResponseEntity.ok(ApiResponse.success(result, "Coupon checked"));
    }
    // ── GET LOYALTY POINTS ───────────────────────
// GET /api/shop/loyalty/{participantId}
    @GetMapping("/loyalty/{participantId}")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getLoyaltyPoints(
            @PathVariable String participantId
    ) {
        int points = loyaltyService.getPoints(participantId);
        double redeemableValue = loyaltyService.getRedeemableValue(participantId);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("points", points);
        result.put("redeemableValue", redeemableValue);
        result.put("canRedeem", loyaltyService.canRedeem(participantId));

        return ResponseEntity.ok(ApiResponse.success(result, "Points fetched"));
    }

    // ── REDEEM POINTS ────────────────────────────
// POST /api/shop/loyalty/redeem
    @PostMapping("/loyalty/redeem")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> redeemPoints(
            @RequestBody java.util.Map<String, Object> body
    ) {
        String participantId = (String) body.get("participantId");
        int pointsToRedeem = (Integer) body.get("points");

        double discount = loyaltyService.redeemPoints(participantId, pointsToRedeem);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("discount", discount);
        result.put("remainingPoints", loyaltyService.getPoints(participantId));

        return ResponseEntity.ok(ApiResponse.success(result, "Points redeemed successfully"));
    }
    // ── CREATE PAYMENT INTENT ─────────────────────
// Frontend calls this before showing payment form
// Returns clientSecret needed to confirm payment on frontend
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
// Frontend calls this on load to initialize Stripe.js
    @GetMapping("/payment/config")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPaymentConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("publishableKey", stripeService.getPublishableKey());
        return ResponseEntity.ok(ApiResponse.success(config, "Payment config"));
    }
}
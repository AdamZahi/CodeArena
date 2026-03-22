package com.codearena.module4_shop.controller;

import com.codearena.module4_shop.dto.ApiResponse;
import com.codearena.module4_shop.dto.PurchaseRequest;
import com.codearena.module4_shop.dto.PurchaseResponse;
import com.codearena.module4_shop.enums.OrderStatus;
import com.codearena.module4_shop.service.ExcelService;
import com.codearena.module4_shop.service.PurchaseService;
import com.codearena.module4_shop.service.QrCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final QrCodeService qrCodeService;
    private final ExcelService excelService;



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
}
package com.codearena.module4_shop.controller;

import com.codearena.module4_shop.dto.ApiResponse;
import com.codearena.module4_shop.dto.ShopItemCreateDto;
import com.codearena.module4_shop.dto.ShopItemDto;
import com.codearena.module4_shop.enums.ItemType;
import com.codearena.module4_shop.enums.OrderStatus;
import com.codearena.module4_shop.service.ExcelService;
import com.codearena.module4_shop.service.PurchaseService;
import com.codearena.module4_shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Slf4j
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final PurchaseService purchaseService;
    private final ExcelService excelService;

    // ── GET ALL PRODUCTS ─────────────────────────
    // GET /api/shop/products
    // Optional: ?category=HOODIE or ?search=code
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ShopItemDto>>> getAllProducts(
            @RequestParam(required = false) ItemType category,
            @RequestParam(required = false) String search
    ) {
        List<ShopItemDto> products;

        if (search != null && !search.isBlank()) {
            // Search by name — métier simple
            products = shopService.searchProducts(search);
        } else if (category != null) {
            // Filter by category — métier simple
            products = shopService.getProductsByCategory(category);
        } else {
            // Return all products
            products = shopService.getAllProducts();
        }

        return ResponseEntity.ok(
                ApiResponse.success(products, "Products fetched successfully")
        );
    }

    // ── GET AVAILABLE PRODUCTS ───────────────────
    // GET /api/shop/products/available
    @GetMapping("/products/available")
    public ResponseEntity<ApiResponse<List<ShopItemDto>>> getAvailableProducts() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        shopService.getAvailableProducts(),
                        "Available products fetched"
                )
        );
    }

    // ── GET LOW STOCK PRODUCTS ───────────────────
    // GET /api/shop/products/low-stock
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/products/low-stock")
    public ResponseEntity<ApiResponse<List<ShopItemDto>>> getLowStockProducts() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        shopService.getLowStockProducts(),
                        "Low stock products fetched"
                )
        );
    }

    // ── GET ONE PRODUCT ──────────────────────────
    // GET /api/shop/products/{id}
    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ShopItemDto>> getProductById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        shopService.getProductById(id),
                        "Product fetched successfully"
                )
        );
    }

    // ── CREATE PRODUCT (Admin) ───────────────────
    // POST /api/shop/products
    // @Valid triggers our DTO validation annotations
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ShopItemDto>> createProduct(
            @Valid @RequestBody ShopItemCreateDto dto
    ) {
        ShopItemDto created = shopService.createProduct(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Product created successfully"));
    }

    // ── UPDATE PRODUCT (Admin) ───────────────────
    // PUT /api/shop/products/{id}
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ShopItemDto>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ShopItemCreateDto dto
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        shopService.updateProduct(id, dto),
                        "Product updated successfully"
                )
        );
    }

    // ── DELETE PRODUCT (Admin) ───────────────────
    // DELETE /api/shop/products/{id}
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID id
    ) {
        shopService.deleteProduct(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Product deleted successfully")
        );
    }

    // ── ADMIN STATS ──────────────────────────────
    // GET /api/shop/stats
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getStats() {
        var stats = new java.util.HashMap<String, Object>();

        // Product stats
        stats.put("totalProducts", shopService.getTotalProductCount());
        stats.put("outOfStock", shopService.getOutOfStockCount());
        stats.put("lowStock", shopService.getLowStockProducts().size());

        // Category breakdown
        var categoryStats = new java.util.HashMap<String, Long>();
        for (ItemType type : ItemType.values()) {
            categoryStats.put(type.name(), shopService.countByCategory(type));
        }
        stats.put("byCategory", categoryStats);

        // Order stats
        stats.put("totalOrders", purchaseService.countByStatus(null));
        stats.put("pendingOrders", purchaseService.countByStatus(OrderStatus.PENDING));
        stats.put("shippedOrders", purchaseService.countByStatus(OrderStatus.SHIPPED));
        stats.put("deliveredOrders", purchaseService.countByStatus(OrderStatus.DELIVERED));
        stats.put("totalRevenue", purchaseService.getTotalRevenue());
        stats.put("bestSellers", purchaseService.getBestSellers());
        stats.put("confirmedOrders", purchaseService.countByStatus(OrderStatus.CONFIRMED));
        stats.put("cancelledOrders", purchaseService.countByStatus(OrderStatus.CANCELLED));
        stats.put("totalOrders",     purchaseService.countAllOrders());
        return ResponseEntity.ok(
                ApiResponse.success(stats, "Stats fetched successfully")
        );
    }

    // ── GLOBAL EXCEPTION HANDLER ─────────────────
    // Returns clean error message when validation fails
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Shop error: {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }
    // GET /api/shop/products/paginated?page=0&size=6&sort=price,asc
    @GetMapping("/products/paginated")
    public ResponseEntity<ApiResponse<Object>> getProductsPaginated(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "6")  int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc")  String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ShopItemDto> result = shopService.getProductsPaginated(pageable);

        var response = new java.util.HashMap<String, Object>();
        response.put("products",    result.getContent());
        response.put("totalPages",  result.getTotalPages());
        response.put("totalItems",  result.getTotalElements());
        response.put("currentPage", result.getNumber());
        response.put("pageSize",    result.getSize());

        return ResponseEntity.ok(ApiResponse.success(response, "Products fetched"));
    }
    // GET /api/shop/products/sorted?sortBy=price&direction=asc
    @GetMapping("/products/sorted")
    public ResponseEntity<ApiResponse<List<ShopItemDto>>> getSortedProducts(
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc")  String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        List<ShopItemDto> products = shopService.getAllProducts();

        return ResponseEntity.ok(
                ApiResponse.success(products, "Sorted products fetched")
        );
    }
    // GET /api/shop/export/products
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/export/products")
    public ResponseEntity<byte[]> exportProducts() throws Exception {
        List<ShopItemDto> products = shopService.getAllProducts();
        byte[] excel = excelService.exportProducts(products);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=products.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excel);
    }
}
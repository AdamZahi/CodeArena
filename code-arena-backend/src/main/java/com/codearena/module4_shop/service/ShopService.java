package com.codearena.module4_shop.service;

import com.codearena.module4_shop.dto.ShopItemCreateDto;
import com.codearena.module4_shop.dto.ShopItemDto;
import com.codearena.module4_shop.enums.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface ShopService {
    Page<ShopItemDto> getProductsPaginated(Pageable pageable);

    // ── CRUD ─────────────────────────────────────

    // Get all products
    List<ShopItemDto> getAllProducts();

    // Get product by ID
    ShopItemDto getProductById(UUID id);

    // Create new product (Admin)
    ShopItemDto createProduct(ShopItemCreateDto dto);

    // Update existing product (Admin)
    ShopItemDto updateProduct(UUID id, ShopItemCreateDto dto);

    // Delete product (Admin)
    void deleteProduct(UUID id);

    // ── MÉTIERS SIMPLES ───────────────────────────

    // Search products by name
    List<ShopItemDto> searchProducts(String name);

    // Filter products by category
    List<ShopItemDto> getProductsByCategory(ItemType category);

    // Get available products only (stock > 0)
    List<ShopItemDto> getAvailableProducts();

    // Get low stock products (stock <= 10)
    List<ShopItemDto> getLowStockProducts();

    // ── MÉTIERS AVANCÉS ───────────────────────────

    // Get product count per category (for admin stats)
    Long countByCategory(ItemType category);

    // Get total product count
    Long getTotalProductCount();

    // Get out of stock count
    Long getOutOfStockCount();
}
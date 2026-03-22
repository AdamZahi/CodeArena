package com.codearena.module4_shop.repository;

import com.codearena.module4_shop.entity.ShopItem;
import com.codearena.module4_shop.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShopItemRepository extends JpaRepository<ShopItem, UUID> {

    // Find products by category
    List<ShopItem> findByCategory(ItemType category);

    // Find products with low stock
    List<ShopItem> findByStockLessThanEqual(Integer stock);

    // Find products by name containing search text
    List<ShopItem> findByNameContainingIgnoreCase(String name);

    // Find available products (stock > 0)
    List<ShopItem> findByStockGreaterThan(Integer stock);

    // Count products by category
    @Query("SELECT COUNT(s) FROM ShopItem s WHERE s.category = :category")
    Long countByCategory(ItemType category);
}
package com.codearena.module4_shop.repository;

import com.codearena.module4_shop.entity.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShopItemRepository extends JpaRepository<ShopItem, UUID> {
    // TODO: Add custom query methods.
}

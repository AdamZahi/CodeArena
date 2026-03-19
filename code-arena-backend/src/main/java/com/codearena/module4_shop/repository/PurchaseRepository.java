package com.codearena.module4_shop.repository;

import com.codearena.module4_shop.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    // TODO: Add custom query methods.
}

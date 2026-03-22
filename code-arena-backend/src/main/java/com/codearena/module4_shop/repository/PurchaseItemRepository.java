package com.codearena.module4_shop.repository;

import com.codearena.module4_shop.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, UUID> {

    // Find items by purchase
    List<PurchaseItem> findByPurchaseId(UUID purchaseId);

    // Best sellers — most ordered products
    @Query("SELECT pi.shopItem.id, pi.shopItem.name, SUM(pi.quantity) as totalSold " +
            "FROM PurchaseItem pi GROUP BY pi.shopItem.id, pi.shopItem.name " +
            "ORDER BY totalSold DESC")
    List<Object[]> findBestSellers();
}
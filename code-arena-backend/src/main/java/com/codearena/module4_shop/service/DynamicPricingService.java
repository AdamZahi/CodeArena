package com.codearena.module4_shop.service;

import com.codearena.module4_shop.entity.ShopItem;
import com.codearena.module4_shop.repository.PurchaseItemRepository;
import com.codearena.module4_shop.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPricingService {

    private final ShopItemRepository shopItemRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ── CALCULATE DYNAMIC PRICE ───────────────────
    public double calculateDynamicPrice(ShopItem product, long totalSold) {
        double basePrice = product.getPrice();
        double finalPrice = basePrice;
        String indicator = "STABLE";

        // ── STOCK-BASED SURGE ─────────────────────
        if (product.getStock() <= 5) {
            finalPrice *= 1.20; // +20% low stock surge
            indicator = "SURGE";
        } else if (product.getStock() <= 10) {
            finalPrice *= 1.10; // +10% low stock
            indicator = "RISING";
        } else if (product.getStock() > 50) {
            finalPrice *= 0.90; // -10% clearance
            indicator = "DEAL";
        }

        // ── DEMAND-BASED SURGE ────────────────────
        if (totalSold >= 10) {
            finalPrice *= 1.15; // +15% high demand
            indicator = indicator.equals("DEAL") ? "POPULAR" : "HOT";
        }

        // Round to 2 decimals
        return Math.round(finalPrice * 100.0) / 100.0;
    }

    // ── GET PRICE INDICATOR ───────────────────────
    public String getPriceIndicator(ShopItem product, long totalSold) {
        if (product.getStock() <= 5 && totalSold >= 10) return "🔥 HOT";
        if (product.getStock() <= 5) return "📈 SURGE";
        if (product.getStock() <= 10) return "⬆ RISING";
        if (totalSold >= 10) return "⭐ POPULAR";
        if (product.getStock() > 50) return "💰 DEAL";
        return "";
    }

    // ── BROADCAST PRICES ──────────────────────────
    // Runs every 30 seconds automatically
    @Scheduled(fixedRate = 30000)
    public void broadcastDynamicPrices() {
        try {
            List<ShopItem> products = shopItemRepository.findAll();
            List<Map<String, Object>> priceUpdates = new ArrayList<>();

            for (ShopItem product : products) {
                long totalSold = purchaseItemRepository.findBestSellers()
                        .stream()
                        .filter(row -> row[0].toString().equals(product.getId().toString()))
                        .mapToLong(row -> ((Number) row[2]).longValue())
                        .findFirst()
                        .orElse(0L);

                double dynamicPrice = calculateDynamicPrice(product, totalSold);
                String indicator = getPriceIndicator(product, totalSold);

                if (dynamicPrice != product.getPrice() || !indicator.isEmpty()) {
                    priceUpdates.add(new java.util.HashMap<String, Object>() {{
                        put("productId", product.getId().toString());
                        put("originalPrice", product.getPrice());
                        put("dynamicPrice", dynamicPrice);
                        put("indicator", indicator);
                        put("changed", dynamicPrice != product.getPrice());
                    }});
                }
            }

            if (!priceUpdates.isEmpty()) {
                messagingTemplate.convertAndSend("/topic/price-updates", priceUpdates);
                log.info("Broadcast {} dynamic price updates", priceUpdates.size());
            }
        } catch (Exception e) {
            log.error("Dynamic pricing broadcast failed: {}", e.getMessage());
        }
    }
}
package com.codearena.module4_shop.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CouponService {

    // ── PREDEFINED COUPONS ────────────────────────
    private static final Map<String, Double> COUPONS = new HashMap<>();
    static {
        COUPONS.put("CODEARENA10", 0.10);
        COUPONS.put("WELCOME20",   0.20);
        COUPONS.put("DEVLIFE15",   0.15);
        COUPONS.put("FREESHIP",    0.05);
    }


    // ── DYNAMIC COUPONS (generated at runtime) ────
    // Stored in memory — survives for the session
    // In production: store in DB table
    private final Map<String, Double> dynamicCoupons = new HashMap<>();

    // ── VALIDATE COUPON ───────────────────────────
    public boolean isValid(String code) {
        if (code == null) return false;
        String upper = code.toUpperCase().trim();
        return COUPONS.containsKey(upper) || dynamicCoupons.containsKey(upper);
    }

    // ── GET DISCOUNT RATE ─────────────────────────
    public double getDiscountRate(String code) {
        if (!isValid(code)) return 0.0;
        String upper = code.toUpperCase().trim();
        if (COUPONS.containsKey(upper)) return COUPONS.get(upper);
        return dynamicCoupons.getOrDefault(upper, 0.0);
    }

    // ── APPLY TO TOTAL ────────────────────────────
    public double applyDiscount(double total, String code) {
        if (!isValid(code)) return total;
        return total * (1 - getDiscountRate(code));
    }

    // ── GENERATE MILESTONE COUPON ─────────────────
    // Called by LoyaltyService when user reaches milestone
    // Returns the generated coupon code
    public String generateMilestoneCoupon(int milestone) {
        // Generate unique code: REWARD100-A3F2, REWARD200-X9K1
        String uniquePart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 4)
                .toUpperCase();
        String code = "REWARD" + milestone + "-" + uniquePart;

        double discountRate = switch (milestone) {
            case 100 -> 0.10;  // 100 pts → 10% off
            case 200 -> 0.15;  // 200 pts → 15% off
            case 500 -> 0.20;  // 500 pts → 20% off (VIP)
            default  -> 0.05;
        };

        dynamicCoupons.put(code, discountRate);
        return code;
    }
}
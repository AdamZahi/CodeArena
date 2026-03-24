package com.codearena.module4_shop.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class CouponService {

    // ── PREDEFINED COUPONS ────────────────────────
    // code → discount rate (0.10 = 10% off, fixed = negative value)
    private static final Map<String, Double> COUPONS = new HashMap<>() {{
        put("CODEARENA10", 0.10); // 10% off
        put("WELCOME20",   0.20); // 20% off
        put("DEVLIFE15",   0.15); // 15% off
        put("FREESHIP",    0.05); // 5% off
    }};

    // ── VALIDATE COUPON ───────────────────────────
    public boolean isValid(String code) {
        return code != null && COUPONS.containsKey(code.toUpperCase().trim());
    }

    // ── GET DISCOUNT RATE ─────────────────────────
    public double getDiscountRate(String code) {
        if (!isValid(code)) return 0.0;
        return COUPONS.get(code.toUpperCase().trim());
    }

    // ── APPLY TO TOTAL ────────────────────────────
    public double applyDiscount(double total, String code) {
        if (!isValid(code)) return total;
        double rate = getDiscountRate(code);
        return total * (1 - rate);
    }
}
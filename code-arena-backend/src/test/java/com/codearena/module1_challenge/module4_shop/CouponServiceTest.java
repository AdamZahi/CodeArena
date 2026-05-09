package com.codearena.module1_challenge.module4_shop;

import com.codearena.module4_shop.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CouponService Unit Tests")
class CouponServiceTest {

    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponService();
    }

    // ── isValid ───────────────────────────────────

    @Test
    @DisplayName("isValid — returns true for valid coupon code")
    void isValid_validCode_returnsTrue() {
        assertThat(couponService.isValid("CODEARENA10")).isTrue();
        assertThat(couponService.isValid("WELCOME20")).isTrue();
        assertThat(couponService.isValid("DEVLIFE15")).isTrue();
        assertThat(couponService.isValid("FREESHIP")).isTrue();
    }

    @Test
    @DisplayName("isValid — case insensitive — lowercase works too")
    void isValid_caseInsensitive() {
        assertThat(couponService.isValid("codearena10")).isTrue();
        assertThat(couponService.isValid("welcome20")).isTrue();
        assertThat(couponService.isValid("Welcome20")).isTrue();
    }

    @Test
    @DisplayName("isValid — returns false for invalid coupon code")
    void isValid_invalidCode_returnsFalse() {
        assertThat(couponService.isValid("FAKECODE")).isFalse();
        assertThat(couponService.isValid("EXPIRED99")).isFalse();
        assertThat(couponService.isValid("")).isFalse();
    }

    @Test
    @DisplayName("isValid — returns false for null code")
    void isValid_nullCode_returnsFalse() {
        assertThat(couponService.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("isValid — trims whitespace before checking")
    void isValid_trimsWhitespace() {
        assertThat(couponService.isValid("  CODEARENA10  ")).isTrue();
    }

    // ── getDiscountRate ───────────────────────────

    @ParameterizedTest
    @CsvSource({
            "CODEARENA10, 0.10",
            "WELCOME20,   0.20",
            "DEVLIFE15,   0.15",
            "FREESHIP,    0.05"
    })
    @DisplayName("getDiscountRate — returns correct rate for each coupon")
    void getDiscountRate_returnsCorrectRate(String code, double expectedRate) {
        assertThat(couponService.getDiscountRate(code)).isEqualTo(expectedRate);
    }

    @Test
    @DisplayName("getDiscountRate — returns 0.0 for invalid coupon")
    void getDiscountRate_invalidCode_returnsZero() {
        assertThat(couponService.getDiscountRate("INVALID")).isEqualTo(0.0);
        assertThat(couponService.getDiscountRate(null)).isEqualTo(0.0);
    }

    // ── applyDiscount ─────────────────────────────

    @Test
    @DisplayName("applyDiscount — CODEARENA10 applies 10% off")
    void applyDiscount_tenPercent() {
        double result = couponService.applyDiscount(100.0, "CODEARENA10");
        assertThat(result).isEqualTo(90.0);
    }

    @Test
    @DisplayName("applyDiscount — WELCOME20 applies 20% off")
    void applyDiscount_twentyPercent() {
        double result = couponService.applyDiscount(100.0, "WELCOME20");
        assertThat(result).isEqualTo(80.0);
    }

    @Test
    @DisplayName("applyDiscount — DEVLIFE15 applies 15% off")
    void applyDiscount_fifteenPercent() {
        double result = couponService.applyDiscount(100.0, "DEVLIFE15");
        assertThat(result).isEqualTo(85.0);
    }

    @Test
    @DisplayName("applyDiscount — FREESHIP applies 5% off")
    void applyDiscount_fivePercent() {
        double result = couponService.applyDiscount(100.0, "FREESHIP");
        assertThat(result).isEqualTo(95.0);
    }

    @Test
    @DisplayName("applyDiscount — invalid coupon returns original total")
    void applyDiscount_invalidCoupon_returnsOriginal() {
        double result = couponService.applyDiscount(100.0, "FAKECODE");
        assertThat(result).isEqualTo(100.0);
    }

    @Test
    @DisplayName("applyDiscount — null coupon returns original total")
    void applyDiscount_nullCoupon_returnsOriginal() {
        double result = couponService.applyDiscount(100.0, null);
        assertThat(result).isEqualTo(100.0);
    }

    @Test
    @DisplayName("applyDiscount — works with real cart total")
    void applyDiscount_realCartTotal() {
        // Cart: Hoodie $39.99 + Mousepad $24.99 = $64.98
        double cartTotal = 64.98;
        double result = couponService.applyDiscount(cartTotal, "WELCOME20");
        assertThat(result).isCloseTo(51.984, within(0.001));
    }
}

package com.codearena.module1_challenge.module4_shop;

import com.codearena.module4_shop.entity.Purchase;
import com.codearena.module4_shop.entity.PurchaseItem;
import com.codearena.module4_shop.entity.ShopItem;
import com.codearena.module4_shop.enums.ItemType;
import com.codearena.module4_shop.enums.OrderStatus;
import com.codearena.module4_shop.repository.PurchaseItemRepository;
import com.codearena.module4_shop.repository.PurchaseRepository;
import com.codearena.module4_shop.repository.ShopItemRepository;
import com.codearena.module4_shop.service.*;
import com.codearena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseService Unit Tests")
class PurchaseServiceImplTest {

    @Mock private PurchaseRepository purchaseRepository;
    @Mock private PurchaseItemRepository purchaseItemRepository;
    @Mock private ShopItemRepository shopItemRepository;
    @Mock private ShopService shopService;
    @Mock private EmailService emailService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private CouponService couponService;
    @Mock private UserRepository userRepository;
    @Mock private LoyaltyService loyaltyService;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private UUID orderId;
    private String participantId;
    private ShopItem mockProduct;
    private Purchase mockPurchase;
    private PurchaseItem mockItem;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        participantId = "google-oauth2|108378133921738621575";

        // Inject @Value field
        ReflectionTestUtils.setField(purchaseService, "adminEmail", "islemazzouz04@gmail.com");

        mockProduct = ShopItem.builder()
                .id(UUID.randomUUID())
                .name("CodeArena Hoodie")
                .price(39.99)
                .stock(10)
                .category(ItemType.HOODIE)
                .build();

        mockItem = PurchaseItem.builder()
                .id(UUID.randomUUID())
                .shopItem(mockProduct)
                .quantity(2)
                .unitPrice(39.99)
                .build();

        mockPurchase = Purchase.builder()
                .id(orderId)
                .participantId(participantId)
                .status(OrderStatus.PENDING)
                .totalPrice(79.98)
                .items(List.of(mockItem))
                .build();
    }

    // ── cancelOrder — OWNERSHIP CHECK ─────────────

    @Test
    @DisplayName("cancelOrder — throws exception when participant doesn't own the order (IDOR protection)")
    void cancelOrder_wrongParticipant_throwsException() {
        // SECURITY TEST: User A tries to cancel User B's order
        String attackerId = "google-oauth2|DIFFERENT_USER";
        when(purchaseRepository.findById(orderId)).thenReturn(Optional.of(mockPurchase));

        assertThatThrownBy(() -> purchaseService.cancelOrder(orderId, attackerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");

        // Verify order was NOT modified
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelOrder — throws exception when order not found")
    void cancelOrder_orderNotFound_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(purchaseRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.cancelOrder(nonExistentId, participantId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("cancelOrder — throws exception when order is not PENDING")
    void cancelOrder_notPending_throwsException() {
        mockPurchase.setStatus(OrderStatus.CONFIRMED);
        when(purchaseRepository.findById(orderId)).thenReturn(Optional.of(mockPurchase));

        assertThatThrownBy(() -> purchaseService.cancelOrder(orderId, participantId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only PENDING orders can be cancelled");
    }

    @Test
    @DisplayName("cancelOrder — restores stock when order is cancelled")
    void cancelOrder_restoresStock() {
        int stockBefore = mockProduct.getStock(); // 10
        when(purchaseRepository.findById(orderId)).thenReturn(Optional.of(mockPurchase));
        when(shopItemRepository.save(any(ShopItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(mockPurchase);
        when(shopService.getProductById(any(UUID.class))).thenReturn(null);

        purchaseService.cancelOrder(orderId, participantId);

        // Stock should be restored: 10 + 2 (quantity) = 12
        assertThat(mockProduct.getStock()).isEqualTo(stockBefore + mockItem.getQuantity());
        verify(shopItemRepository, times(1)).save(mockProduct);
    }

    @Test
    @DisplayName("cancelOrder — sets status to CANCELLED")
    void cancelOrder_setsStatusCancelled() {
        when(purchaseRepository.findById(orderId)).thenReturn(Optional.of(mockPurchase));
        when(shopItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> {
            Purchase p = inv.getArgument(0);
            assertThat(p.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            return p;
        });
        when(shopService.getProductById(any())).thenReturn(null);

        purchaseService.cancelOrder(orderId, participantId);

        verify(purchaseRepository, times(1)).save(any(Purchase.class));
    }

    // ── DISCOUNT ALGORITHM ────────────────────────
    // Tests the private applyQuantityDiscount via reflection

    @ParameterizedTest
    @CsvSource({
            "1, 39.99, 39.99",   // no discount — single item
            "2, 39.99, 37.99",   // 5% off — buy 2
            "3, 39.99, 35.99",   // 10% off — buy 3
            "5, 39.99, 31.99",   // 20% off — buy 5+
            "10, 39.99, 31.99",  // 20% off — buy 10
    })
    @DisplayName("applyQuantityDiscount — correct discount per quantity tier")
    void applyQuantityDiscount_correctTiers(int quantity, double price, double expected) {
        double result = ReflectionTestUtils.invokeMethod(
                purchaseService, "applyQuantityDiscount", price, quantity
        );
        assertThat(result).isCloseTo(expected, within(0.01));
    }

    @Test
    @DisplayName("applyQuantityDiscount — single item has no discount")
    void applyQuantityDiscount_singleItem_noDiscount() {
        double result = ReflectionTestUtils.invokeMethod(
                purchaseService, "applyQuantityDiscount", 100.0, 1
        );
        assertThat(result).isEqualTo(100.0);
    }

    @Test
    @DisplayName("applyQuantityDiscount — quantity 5 gets 20% off")
    void applyQuantityDiscount_bulkBuyer_twentyPercent() {
        double result = ReflectionTestUtils.invokeMethod(
                purchaseService, "applyQuantityDiscount", 100.0, 5
        );
        assertThat(result).isEqualTo(80.0);
    }

    // ── getTotalRevenue ───────────────────────────

    @Test
    @DisplayName("getTotalRevenue — returns revenue from repository")
    void getTotalRevenue_returnsRevenue() {
        when(purchaseRepository.calculateTotalRevenue()).thenReturn(1500.0);

        Double result = purchaseService.getTotalRevenue();

        assertThat(result).isEqualTo(1500.0);
    }

    @Test
    @DisplayName("getTotalRevenue — returns 0.0 when no orders exist")
    void getTotalRevenue_noOrders_returnsZero() {
        when(purchaseRepository.calculateTotalRevenue()).thenReturn(null);

        Double result = purchaseService.getTotalRevenue();

        assertThat(result).isEqualTo(0.0);
    }

    // ── countByStatus ─────────────────────────────

    @Test
    @DisplayName("countByStatus — returns count for specific status")
    void countByStatus_specificStatus() {
        when(purchaseRepository.countByStatus(OrderStatus.PENDING)).thenReturn(5L);

        Long result = purchaseService.countByStatus(OrderStatus.PENDING);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("countByStatus — null status returns total count")
    void countByStatus_null_returnsTotalCount() {
        when(purchaseRepository.count()).thenReturn(42L);

        Long result = purchaseService.countByStatus(null);

        assertThat(result).isEqualTo(42L);
    }
}

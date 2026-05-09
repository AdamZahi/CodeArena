package com.codearena.module1_challenge.module4_shop;

import com.codearena.module4_shop.dto.ShopItemCreateDto;
import com.codearena.module4_shop.dto.ShopItemDto;
import com.codearena.module4_shop.entity.ShopItem;
import com.codearena.module4_shop.enums.ItemType;
import com.codearena.module4_shop.repository.ShopItemRepository;
import com.codearena.module4_shop.service.ShopServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopService Unit Tests")
class ShopServiceImplTest {

    @Mock
    private ShopItemRepository shopItemRepository;

    @InjectMocks
    private ShopServiceImpl shopService;

    private ShopItem mockProduct;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        mockProduct = ShopItem.builder()
                .id(productId)
                .name("CodeArena Hoodie")
                .description("Premium black hoodie with CodeArena logo")
                .price(39.99)
                .stock(50)
                .imageUrl("https://example.com/hoodie.jpg")
                .category(ItemType.HOODIE)
                .build();
    }

    // ── GET ALL PRODUCTS ──────────────────────────

    @Test
    @DisplayName("getAllProducts — returns all products as DTOs")
    void getAllProducts_returnsAllProducts() {
        when(shopItemRepository.findAll()).thenReturn(List.of(mockProduct));

        List<ShopItemDto> result = shopService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("CodeArena Hoodie");
        assertThat(result.get(0).getPrice()).isEqualTo(39.99);
        verify(shopItemRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllProducts — returns empty list when no products")
    void getAllProducts_emptyList() {
        when(shopItemRepository.findAll()).thenReturn(List.of());

        List<ShopItemDto> result = shopService.getAllProducts();

        assertThat(result).isEmpty();
    }

    // ── GET PRODUCT BY ID ─────────────────────────

    @Test
    @DisplayName("getProductById — returns product when found")
    void getProductById_found() {
        when(shopItemRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

        ShopItemDto result = shopService.getProductById(productId);

        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getName()).isEqualTo("CodeArena Hoodie");
        assertThat(result.getStock()).isEqualTo(50);
    }

    @Test
    @DisplayName("getProductById — throws RuntimeException when not found")
    void getProductById_notFound_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(shopItemRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.getProductById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
    }

    // ── CREATE PRODUCT ────────────────────────────

    @Test
    @DisplayName("createProduct — saves and returns new product")
    void createProduct_savesProduct() {
        ShopItemCreateDto dto = new ShopItemCreateDto();
        dto.setName("CodeArena Mousepad");
        dto.setDescription("XL desk mat 90x40cm");
        dto.setPrice(24.99);
        dto.setStock(100);
        dto.setImageUrl("https://example.com/mousepad.jpg");
        dto.setCategory(ItemType.MOUSEPAD);

        ShopItem saved = ShopItem.builder()
                .id(UUID.randomUUID())
                .name("CodeArena Mousepad")
                .description("XL desk mat 90x40cm")
                .price(24.99)
                .stock(100)
                .imageUrl("https://example.com/mousepad.jpg")
                .category(ItemType.MOUSEPAD)
                .build();

        when(shopItemRepository.save(any(ShopItem.class))).thenReturn(saved);

        ShopItemDto result = shopService.createProduct(dto);

        assertThat(result.getName()).isEqualTo("CodeArena Mousepad");
        assertThat(result.getPrice()).isEqualTo(24.99);
        assertThat(result.getStock()).isEqualTo(100);
        verify(shopItemRepository, times(1)).save(any(ShopItem.class));
    }

    @Test
    @DisplayName("createProduct — sets all fields correctly")
    void createProduct_setsAllFields() {
        ShopItemCreateDto dto = new ShopItemCreateDto();
        dto.setName("Dark Mode Mug");
        dto.setDescription("Heat-sensitive mug");
        dto.setPrice(17.99);
        dto.setStock(30);
        dto.setCategory(ItemType.MUG);

        when(shopItemRepository.save(any(ShopItem.class))).thenAnswer(inv -> {
            ShopItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        ShopItemDto result = shopService.createProduct(dto);

        assertThat(result.getCategory()).isEqualTo(ItemType.MUG);
        assertThat(result.getPrice()).isEqualTo(17.99);
    }

    // ── UPDATE PRODUCT ────────────────────────────

    @Test
    @DisplayName("updateProduct — updates existing product fields")
    void updateProduct_updatesFields() {
        ShopItemCreateDto dto = new ShopItemCreateDto();
        dto.setName("CodeArena Hoodie UPDATED");
        dto.setDescription("Updated description");
        dto.setPrice(44.99);
        dto.setStock(25);
        dto.setCategory(ItemType.HOODIE);

        when(shopItemRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(shopItemRepository.save(any(ShopItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ShopItemDto result = shopService.updateProduct(productId, dto);

        assertThat(result.getName()).isEqualTo("CodeArena Hoodie UPDATED");
        assertThat(result.getPrice()).isEqualTo(44.99);
        assertThat(result.getStock()).isEqualTo(25);
    }

    @Test
    @DisplayName("updateProduct — throws exception when product not found")
    void updateProduct_notFound_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        ShopItemCreateDto dto = new ShopItemCreateDto();
        when(shopItemRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.updateProduct(nonExistentId, dto))
                .isInstanceOf(RuntimeException.class);
    }

    // ── DELETE PRODUCT ────────────────────────────

    @Test
    @DisplayName("deleteProduct — deletes existing product")
    void deleteProduct_deletesProduct() {
        when(shopItemRepository.existsById(productId)).thenReturn(true);
        doNothing().when(shopItemRepository).deleteById(productId);

        shopService.deleteProduct(productId);

        verify(shopItemRepository, times(1)).deleteById(productId);
    }

    @Test
    @DisplayName("deleteProduct — throws exception when product not found")
    void deleteProduct_notFound_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(shopItemRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> shopService.deleteProduct(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
    }
}
package com.codearena.module4_shop.service;

import com.codearena.module4_shop.dto.ShopItemCreateDto;
import com.codearena.module4_shop.dto.ShopItemDto;
import com.codearena.module4_shop.entity.ShopItem;
import com.codearena.module4_shop.enums.ItemType;
import com.codearena.module4_shop.exception.ProductNotFoundException;
import com.codearena.module4_shop.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopItemRepository shopItemRepository;

    // ── CRUD ─────────────────────────────────────

    @Override
    public List<ShopItemDto> getAllProducts() {
        log.info("Fetching all products");
        return shopItemRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ShopItemDto getProductById(UUID id) {
        log.info("Fetching product with id: {}", id);
        ShopItem item = shopItemRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id.toString()));
        return toDto(item);
    }

    @Override
    public ShopItemDto createProduct(ShopItemCreateDto dto) {
        log.info("Creating new product: {}", dto.getName());
        ShopItem item = ShopItem.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .imageUrl(dto.getImageUrl())
                .category(dto.getCategory())
                .build();
        ShopItem saved = shopItemRepository.save(item);
        return toDto(saved);
    }

    @Override
    public ShopItemDto updateProduct(UUID id, ShopItemCreateDto dto) {
        log.info("Updating product with id: {}", id);
        ShopItem item = shopItemRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id.toString()));

        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setStock(dto.getStock());
        item.setImageUrl(dto.getImageUrl());
        item.setCategory(dto.getCategory());

        ShopItem updated = shopItemRepository.save(item);
        return toDto(updated);
    }

    @Override
    public void deleteProduct(UUID id) {
        log.info("Deleting product with id: {}", id);
        if (!shopItemRepository.existsById(id)) {
            throw new ProductNotFoundException(id.toString());
        }
        shopItemRepository.deleteById(id);
    }

    // ── MÉTIERS SIMPLES ───────────────────────────

    @Override
    public List<ShopItemDto> searchProducts(String name) {
        log.info("Searching products with name: {}", name);
        return shopItemRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShopItemDto> getProductsByCategory(ItemType category) {
        log.info("Filtering products by category: {}", category);
        return shopItemRepository.findByCategory(category)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShopItemDto> getAvailableProducts() {
        return shopItemRepository.findByStockGreaterThan(0)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShopItemDto> getLowStockProducts() {
        log.info("Fetching low stock products");
        return shopItemRepository.findByStockLessThanEqual(10)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ── MÉTIERS AVANCÉS ───────────────────────────

    @Override
    public Long countByCategory(ItemType category) {
        return shopItemRepository.countByCategory(category);
    }

    @Override
    public Long getTotalProductCount() {
        return shopItemRepository.count();
    }

    @Override
    public Long getOutOfStockCount() {
        return shopItemRepository.findByStockLessThanEqual(0)
                .stream().count();
    }
    @Override
    public void saveEcoScore(UUID productId, int ecoScore) {
        ShopItem item = shopItemRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId.toString()));
        item.setEcoScore(ecoScore);
        shopItemRepository.save(item);
        log.info("Saved eco score {}/10 for product: {}", ecoScore, item.getName());
    }

    // ── HELPER — Entity to DTO ────────────────────
    private ShopItemDto toDto(ShopItem item) {
        return ShopItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .stock(item.getStock())
                .imageUrl(item.getImageUrl())
                .category(item.getCategory())
                .ecoScore(item.getEcoScore())
                .createdAt(item.getCreatedAt())
                .build();
    }
    @Override
    public Page<ShopItemDto> getProductsPaginated(Pageable pageable) {
        return shopItemRepository.findAll(pageable)
                .map(this::toDto);
    }

    @Override
    //implementing code in the back also so that backend is the one calling Flask.
    public ShopItemDto analyzeAndSaveEcoScore(UUID productId) {
        ShopItem item = shopItemRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId.toString()));

        // Backend calls Flask — not Angular
        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> flaskRequest = new HashMap<>();
            flaskRequest.put("productId", item.getId().toString());
            flaskRequest.put("productName", item.getName());
            flaskRequest.put("category", item.getCategory().name());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://localhost:5000/api/eco-score",
                    flaskRequest,
                    Map.class
            );

            Map<?, ?> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("score")) {
                int score = ((Number) responseBody.get("score")).intValue();
                item.setEcoScore(score);
                shopItemRepository.save(item);
                log.info("AI eco score {} saved for product {}", score, item.getName());
            } else {
                log.warn("Flask returned empty response for product {}", item.getName());
            }

        } catch (Exception e) {
            log.warn("Flask AI unavailable: {}", e.getMessage());
        }

        return toDto(item);
    }
}

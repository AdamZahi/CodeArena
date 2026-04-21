package com.codearena.module4_shop.dto;

import com.codearena.module4_shop.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopItemDto {

    private UUID id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String imageUrl;
    private ItemType category;
    private Integer ecoScore;
    // AI eco score (1-10) — null if not yet scored
    // Used by frontend to show badge and alternative suggestions
    // Used by admin panel to flag low-eco products
    private LocalDateTime createdAt;

}

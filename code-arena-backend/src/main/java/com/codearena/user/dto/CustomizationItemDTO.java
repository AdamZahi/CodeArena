package com.codearena.user.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizationItemDTO {
    private Long id;
    private String itemType;
    private String itemKey;
    private String label;
    private String imageUrl;
    private String rarity;
    private String description;
    private String unlockType;
    private Integer unlockThreshold;
    private Boolean isDefault;
}

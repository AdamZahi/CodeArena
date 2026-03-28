package com.codearena.user.dto;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUnlockDTO {
    private Long id;
    private String userId;
    private String itemType;
    private String itemKey;
    private Instant unlockedAt;
    private String acquisitionSource;

    // Joined info from CustomizationItem
    private String label;
    private String imageUrl;
    private String rarity;
}

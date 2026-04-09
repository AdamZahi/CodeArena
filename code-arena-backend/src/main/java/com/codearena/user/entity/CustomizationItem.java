package com.codearena.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customization_items")
public class CustomizationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_type")
    private String itemType; // ICON, BORDER, BADGE, TITLE, BANNER

    @Column(name = "item_key")
    private String itemKey; // unique identifier like "icon_fire", "border_gold"

    private String label; // Display name: "Fire Icon", "Gold Border"

    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl; // URL or base64 image

    private String rarity; // COMMON, UNCOMMON, RARE, EPIC, LEGENDARY

    private String description;

    // Unlock criteria
    private String unlockType; // DEFAULT, LEVEL, XP, CHALLENGES_SOLVED, ACHIEVEMENT, PURCHASE

    @Builder.Default
    private Integer unlockThreshold = 0; // e.g. level 5, 500 xp, 10 challenges

    @Builder.Default
    private Boolean isDefault = false; // true = everyone gets it from the start
}

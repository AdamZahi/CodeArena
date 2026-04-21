package com.codearena.module4_shop.entity;

import com.codearena.module4_shop.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shop_items")
public class ShopItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer stock;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType category;


    @Column(name = "eco_score")
    private Integer ecoScore;
// AI-generated eco score (1-10) from Flask NLP model
// Stored in DB so we don't re-score on every page load
// null = not yet scored

    @CreationTimestamp
    private LocalDateTime createdAt;


}
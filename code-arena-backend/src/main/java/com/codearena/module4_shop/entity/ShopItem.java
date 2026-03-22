package com.codearena.module4_shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ShopItem {
    @Id
    private UUID id;

    private String name;

    private String description;

    private String type;

    private String price;

    private String imageUrl;

    private String available;
}

package com.codearena.module4_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loyalty_points")
public class LoyaltyPoints {

    @Id
    @Column(nullable = false, unique = true)
    private String participantId;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 0;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

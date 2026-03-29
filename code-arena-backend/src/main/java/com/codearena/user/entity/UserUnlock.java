package com.codearena.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_unlocks")
public class UserUnlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId; // keycloakId

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(name = "item_key", nullable = false)
    private String itemKey;

    @CreationTimestamp
    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    @Column(name = "acquisition_source")
    private String acquisitionSource; // "DEFAULT", "LEVEL_UP", "XP_MILESTONE", "CHALLENGE_SOLVED", "PURCHASE"
}

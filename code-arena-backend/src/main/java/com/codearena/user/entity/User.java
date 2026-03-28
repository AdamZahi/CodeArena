package com.codearena.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String keycloakId;

    private String email;
    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    private String avatarUrl;
    private String bio;

    private boolean isActive;

    @Builder.Default
    private Long totalXp = 0L;

    @Builder.Default
    private Integer currentLevel = 1;

    private Integer level;

    private String nickname;

    private String rankTier;
    private String rankDivision;

    @Builder.Default
    private Integer leaguePoints = 0;

    @Builder.Default
    private Integer matchesPlayed = 0;

    @Builder.Default
    private Integer matchesWon = 0;

    @Builder.Default
    private Integer honorLevel = 2;

    @Builder.Default
    private String activeIconId = "default_icon";

    @Builder.Default
    private String activeBorderId = "default_border";

    private String activeTitle;

    private String activeBadge1;
    private String activeBadge2;
    private String activeBadge3;

    @CreationTimestamp
    private Instant createdAt;
}

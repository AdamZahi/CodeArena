package com.codearena.module2_battle.entity;

import com.codearena.module2_battle.enums.PlayerTier;
import jakarta.persistence.*;
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
@Entity
@Table(name = "player_rating", uniqueConstraints = {
        @UniqueConstraint(name = "uq_rating_user_season", columnNames = {"user_id", "season_id"})
}, indexes = {
        @Index(name = "idx_rating_season_elo", columnList = "season_id, elo")
})
public class PlayerRating {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "season_id", nullable = false)
    private String seasonId;

    @Column(nullable = false)
    @Builder.Default
    private Integer elo = 1000;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlayerTier tier = PlayerTier.BRONZE;

    @Column(nullable = false)
    @Builder.Default
    private Integer wins = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer losses = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer draws = 0;

    @Column(name = "win_streak", nullable = false)
    @Builder.Default
    private Integer winStreak = 0;

    @Column(name = "best_win_streak", nullable = false)
    @Builder.Default
    private Integer bestWinStreak = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

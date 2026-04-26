package com.codearena.module2_battle.admin.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "battle_config")
public class BattleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "max_participants", nullable = false)
    @Builder.Default
    private Integer maxParticipants = 2;

    @Column(name = "time_limit_minutes", nullable = false)
    @Builder.Default
    private Integer timeLimitMinutes = 30;

    /** JSON array of language slugs, e.g. ["java","python","cpp"]. */
    @Column(name = "allowed_languages", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String allowedLanguages = "[\"java\",\"python\",\"cpp\"]";

    @Column(name = "xp_reward_winner", nullable = false)
    @Builder.Default
    private Integer xpRewardWinner = 100;

    @Column(name = "xp_reward_loser", nullable = false)
    @Builder.Default
    private Integer xpRewardLoser = 20;

    @Column(name = "min_rank_required")
    private String minRankRequired;

    @Column(name = "allow_spectators", nullable = false)
    @Builder.Default
    private Boolean allowSpectators = false;

    @Column(name = "auto_close_abandoned_after_minutes", nullable = false)
    @Builder.Default
    private Integer autoCloseAbandonedAfterMinutes = 10;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;
}

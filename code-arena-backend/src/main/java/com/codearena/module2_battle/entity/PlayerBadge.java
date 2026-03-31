package com.codearena.module2_battle.entity;

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
@Table(name = "player_badge", indexes = {
        @Index(name = "idx_pb_user_id", columnList = "user_id"),
        @Index(name = "idx_pb_badge_id", columnList = "badge_id"),
        @Index(name = "idx_pb_participant_id", columnList = "participant_id")
})
public class PlayerBadge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "badge_id", nullable = false)
    private String badgeId;

    // FK to battle_participant.id; null for non-battle badges
    @Column(name = "participant_id")
    private String participantId;

    @Column(name = "awarded_at")
    private LocalDateTime awardedAt;
}

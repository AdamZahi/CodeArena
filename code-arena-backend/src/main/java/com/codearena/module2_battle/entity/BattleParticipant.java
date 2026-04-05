package com.codearena.module2_battle.entity;

import com.codearena.module2_battle.enums.ParticipantRole;
import jakarta.persistence.*;
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
@Table(name = "battle_participant", uniqueConstraints = {
        @UniqueConstraint(name = "uq_participant_room_user", columnNames = {"room_id", "user_id"})
}, indexes = {
        @Index(name = "idx_participant_room_role", columnList = "room_id, role"),
        @Index(name = "idx_participant_user_id", columnList = "user_id")
})
public class BattleParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "room_id")
    private String roomId;

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ParticipantRole role = ParticipantRole.PLAYER;

    @Column(name = "is_ready", nullable = false)
    @Builder.Default
    private Boolean isReady = false;

    @Column(name = "elo_change")
    private Integer eloChange;

    private Integer score;

    @Column(name = "`rank`")
    private Integer rank;

    @CreationTimestamp
    private Instant joinedAt;
}

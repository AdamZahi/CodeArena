package com.codearena.module2_battle.entity;

import com.codearena.module2_battle.enums.BattleMode;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "battle_room", indexes = {
        @Index(name = "idx_battle_room_status_public", columnList = "status, is_public"),
        @Index(name = "idx_battle_room_host_id", columnList = "host_id")
})
public class BattleRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "host_id")
    private String hostId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BattleMode mode = BattleMode.DUEL;

    @Column(name = "max_players", nullable = false)
    @Builder.Default
    private Integer maxPlayers = 2;

    @Column(name = "challenge_count", nullable = false)
    @Builder.Default
    private Integer challengeCount = 1;

    @Column(name = "invite_token", unique = true)
    private String inviteToken;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BattleRoomStatus status = BattleRoomStatus.WAITING;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @CreationTimestamp
    private Instant createdAt;
}

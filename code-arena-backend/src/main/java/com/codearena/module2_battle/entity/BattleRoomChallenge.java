package com.codearena.module2_battle.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "battle_room_challenge", uniqueConstraints = {
        @UniqueConstraint(name = "uq_room_challenge_position", columnNames = {"room_id", "position"})
}, indexes = {
        @Index(name = "idx_brc_room_id", columnList = "room_id"),
        @Index(name = "idx_brc_challenge_id", columnList = "challenge_id")
})
public class BattleRoomChallenge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "challenge_id", nullable = false)
    private String challengeId;

    // 1-based order of challenge in the room
    @Column(nullable = false)
    private Integer position;
}

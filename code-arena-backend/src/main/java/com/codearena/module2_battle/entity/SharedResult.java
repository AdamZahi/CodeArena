package com.codearena.module2_battle.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shared_result", indexes = {
        @Index(name = "idx_shared_result_room_user", columnList = "battle_room_id,requested_by_user_id"),
        @Index(name = "idx_shared_result_expires", columnList = "expires_at")
})
public class SharedResult {

    @Id
    @Column(length = 8)
    private String shareToken;

    @Column(name = "battle_room_id", nullable = false)
    private String battleRoomId;

    @Column(name = "requested_by_user_id", nullable = false)
    private String requestedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}

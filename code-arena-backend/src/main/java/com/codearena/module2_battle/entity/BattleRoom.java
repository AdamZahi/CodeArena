package com.codearena.module2_battle.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
public class BattleRoom {
    @Id
    private UUID id;

    private String roomKey;

    private String hostId;

    private String challengeId;

    private String status;

    @CreationTimestamp
    private Instant createdAt;
}

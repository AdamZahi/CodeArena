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
public class BattleParticipant {
    @Id
    private UUID id;

    private String roomId;

    private String userId;

    private String joinedAt;

    private String score;

    private String rank;
}

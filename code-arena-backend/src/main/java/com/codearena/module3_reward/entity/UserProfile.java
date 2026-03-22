package com.codearena.module3_reward.entity;

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
public class UserProfile {
    @Id
    private UUID id;

    private String userId;

    private String totalXp;

    private String currentRankId;

    private String bio;

    private String avatarUrl;
}

package com.codearena.module2_battle.entity;

import com.codearena.module2_battle.enums.DailyEntryStatus;
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
@Table(name = "daily_entry", uniqueConstraints = {
        @UniqueConstraint(name = "uq_daily_entry_user_day", columnNames = {"user_id", "daily_challenge_id"})
}, indexes = {
        @Index(name = "idx_de_daily_challenge_id", columnList = "daily_challenge_id"),
        @Index(name = "idx_de_user_id", columnList = "user_id")
})
public class DailyEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "daily_challenge_id", nullable = false)
    private String dailyChallengeId;

    private Integer score;

    @Column(name = "time_seconds")
    private Integer timeSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DailyEntryStatus status = DailyEntryStatus.IN_PROGRESS;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}

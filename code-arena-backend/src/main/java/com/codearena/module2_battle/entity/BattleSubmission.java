package com.codearena.module2_battle.entity;

import com.codearena.module2_battle.enums.BattleSubmissionStatus;
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
@Table(name = "battle_submission", indexes = {
        @Index(name = "idx_bs_participant_id", columnList = "participant_id"),
        @Index(name = "idx_bs_participant_room_challenge", columnList = "participant_id, room_challenge_id")
})
public class BattleSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "participant_id", nullable = false)
    private String participantId;

    @Column(name = "room_challenge_id", nullable = false)
    private String roomChallengeId;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BattleSubmissionStatus status = BattleSubmissionStatus.PENDING;

    // Increments per participant per challenge; never reset
    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Column(name = "memory_kb")
    private Integer memoryKb;

    // Computed after judging; null until then
    private Integer score;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    public void onPrePersist() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        if (attemptNumber == null || attemptNumber < 1) {
            attemptNumber = 1;
        }
    }
}

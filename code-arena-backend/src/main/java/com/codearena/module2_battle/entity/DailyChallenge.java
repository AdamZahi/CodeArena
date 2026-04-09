package com.codearena.module2_battle.entity;

import com.codearena.module2_battle.converter.StringListJsonConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_challenge", uniqueConstraints = {
        @UniqueConstraint(name = "uq_daily_challenge_date", columnNames = {"challenge_date"})
})
public class DailyChallenge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "binary(16)")
    private UUID id;

    @Column(name = "challenge_date", nullable = false)
    private LocalDate challengeDate;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "challenge_ids", nullable = false, columnDefinition = "JSON")
    private List<String> challengeIds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

package com.codearena.module1_challenge.entity;

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
public class Submission {
    @Id
    private UUID id;

    private String userId;

    private String challengeId;

    private String code;

    private String language;

    private String status;

    private String xpEarned;

    private String submittedAt;
}

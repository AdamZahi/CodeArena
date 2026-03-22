package com.codearena.module1_challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDto {
    private UUID id;
    private UUID challengeId;
    private String userId;
    private String code;
    private String language;
    private String status;
    private String xpEarned;
    private Instant submittedAt;
    private Float executionTime;
    private Float memoryUsed;
    private String errorOutput;
    private String challengeTitle;
}

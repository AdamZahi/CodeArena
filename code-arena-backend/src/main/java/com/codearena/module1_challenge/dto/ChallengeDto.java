package com.codearena.module1_challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDto {
    private UUID id;
    private String title;
    private String description;
    private String difficulty;
    private String tags;
    private String authorId;
    private Instant createdAt;
    private List<TestCaseDto> testCases;
}

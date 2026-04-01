package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResultResponse {
    private String submissionId;
    private String roomChallengeId;
    private String status;
    private int attemptNumber;
    private Integer runtimeMs;
    private Integer memoryKb;
    private String feedback;
    private boolean isAccepted;
}

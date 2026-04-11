package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplaySubmissionResponse {
    private String submissionId;
    private String participantId;
    private String username;
    private int challengePosition;
    private String language;
    private String status;
    private int attemptNumber;
    private Integer runtimeMs;
    private Integer memoryKb;
    private long secondsFromStart;
}

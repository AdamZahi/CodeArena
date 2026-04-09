package com.codearena.module2_battle.dto;

import com.codearena.module2_battle.enums.TestCaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseProgressEvent {
    private String submissionId;
    private int testCaseIndex;
    private int totalTestCases;
    private TestCaseStatus status;
    private String errorType;
}

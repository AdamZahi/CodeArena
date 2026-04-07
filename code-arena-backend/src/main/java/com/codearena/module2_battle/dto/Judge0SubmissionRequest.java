package com.codearena.module2_battle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for Judge0 code execution requests. Not exposed via API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Judge0SubmissionRequest {

    @JsonProperty("language_id")
    private int languageId;

    @JsonProperty("source_code")
    private String sourceCode;

    private String stdin;

    @JsonProperty("expected_output")
    private String expectedOutput;

    @JsonProperty("cpu_time_limit")
    @Builder.Default
    private int timeLimitSeconds = 5;

    @JsonProperty("memory_limit")
    @Builder.Default
    private int memoryLimitKb = 262144;
}

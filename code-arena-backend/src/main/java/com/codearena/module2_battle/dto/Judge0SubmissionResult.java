package com.codearena.module2_battle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for Judge0 submission results. Not exposed via API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Judge0SubmissionResult {

    private String token;

    @JsonProperty("status")
    private Judge0Status status;

    private Double time;

    private Integer memory;

    private String stdout;

    private String stderr;

    @JsonProperty("compile_output")
    private String compileOutput;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Judge0Status {
        private int id;
        private String description;
    }
}

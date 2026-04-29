package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for Piston code execution requests. Not exposed via API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PistonExecutionRequest {

    private String language;

    private String version;

    private String sourceCode;

    /** Optional filename sent to Piston (e.g. "Main.java"). Required for Java so the runner
     *  can derive the class name from the filename. */
    private String fileName;

    private String stdin;

    @Builder.Default
    private int runTimeoutMs = 3000;

    @Builder.Default
    private int memoryLimitBytes = 256_000_000;
}

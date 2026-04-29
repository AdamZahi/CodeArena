package com.codearena.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Normalized result DTO shared by all code execution engines (Piston, Judge0).
 * Callers never need to know which engine produced the result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    /** Standard output from the executed code (plain text, never base64). */
    private String stdout;

    /** Standard error from the executed code (plain text, never base64). */
    private String stderr;

    /**
     * Exit code of the executed program.
     * 0 = success (maps to Piston's run.code and Judge0 status 3/Accepted).
     * Non-zero = failure.
     */
    private int exitCode;

    /** Compilation error message, or null if compilation succeeded. */
    private String compileError;

    /** Which engine executed this request: "PISTON" or "JUDGE0". */
    private String engineUsed;

    /** Wall-clock execution time in milliseconds. */
    private long executionTimeMs;
}

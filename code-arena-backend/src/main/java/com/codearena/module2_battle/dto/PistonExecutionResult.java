package com.codearena.module2_battle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for Piston execution results. Not exposed via API.
 * Piston returns results synchronously — no polling needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PistonExecutionResult {

    /** Process exit code. 0 = success. */
    private int exitCode;

    /** Standard output from the program. */
    private String stdout;

    /** Standard error output. */
    private String stderr;

    /** Compilation output (if applicable). */
    private String compileOutput;

    /** CPU time in milliseconds. */
    private Integer cpuTimeMs;

    /** Wall-clock time in milliseconds. */
    private Integer wallTimeMs;

    /** Memory usage in bytes. */
    private Long memoryBytes;

    /** Whether a signal killed the process (e.g. SIGKILL for OOM). */
    private String signal;

    /** Non-null when the Piston call itself failed (network error, etc.). */
    private String errorMessage;

    /** Memory usage converted to KB for compatibility. */
    public Integer getMemoryKb() {
        return memoryBytes != null ? (int) (memoryBytes / 1024) : null;
    }

    /** CPU time as seconds (Double) for compatibility with existing submission logic. */
    public Double getTimeSeconds() {
        return cpuTimeMs != null ? cpuTimeMs / 1000.0 : null;
    }
}

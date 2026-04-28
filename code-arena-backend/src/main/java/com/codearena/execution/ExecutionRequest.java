package com.codearena.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input DTO for code execution requests.
 * Accepts both Judge0 numeric language IDs (e.g., "62") and
 * language names (e.g., "java", "python") — the LanguageRegistry resolves both.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequest {

    /** Source code to execute. */
    private String sourceCode;

    /**
     * Language identifier — can be a name ("java", "python", "py")
     * or a Judge0 numeric ID string ("62", "71").
     */
    private String language;

    /** Optional standard input to feed to the program. */
    private String stdin;

    /**
     * Optional expected output — used by Judge0 for built-in comparison.
     * Piston does not use this field (comparison is done by the caller).
     */
    private String expectedOutput;
}

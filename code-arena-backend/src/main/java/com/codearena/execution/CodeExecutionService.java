package com.codearena.execution;

/**
 * Abstraction layer for code execution engines.
 * Callers interact only with this interface — they never know
 * whether Piston or Judge0 handled the request.
 */
public interface CodeExecutionService {

    /**
     * Executes code and returns a normalized result.
     *
     * @param request the execution request containing source code, language, and optional stdin
     * @return normalized execution result with stdout, stderr, exitCode, and engine info
     */
    ExecutionResult execute(ExecutionRequest request);
}

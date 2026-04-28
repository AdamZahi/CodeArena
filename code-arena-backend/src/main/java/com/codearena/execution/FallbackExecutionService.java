package com.codearena.execution;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * Orchestrator that tries Piston first, then falls back to Judge0.
 *
 * Fallback triggers:
 * <ul>
 *   <li>{@code execution.piston.enabled = false}</li>
 *   <li>Language not in Piston manifest</li>
 *   <li>Circuit breaker OPEN</li>
 *   <li>ConnectException, SocketTimeoutException, HTTP 5xx from Piston</li>
 * </ul>
 *
 * Marked {@code @Primary} so that any injection of {@link CodeExecutionService}
 * gets this orchestrator rather than the individual engine beans.
 */
@Slf4j
@Primary
@Service
public class FallbackExecutionService implements CodeExecutionService {

    private final PistonExecutionService pistonService;
    private final Judge0ExecutionService judge0Service;
    private final ExecutionConfig config;
    private final LanguageRegistry languageRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /** Tracks the last engine used — exposed via the health indicator. */
    private volatile String lastEngineUsed = "NONE";

    public FallbackExecutionService(
            PistonExecutionService pistonService,
            Judge0ExecutionService judge0Service,
            ExecutionConfig config,
            LanguageRegistry languageRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.pistonService = pistonService;
        this.judge0Service = judge0Service;
        this.config = config;
        this.languageRegistry = languageRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        // Gate 1: Piston disabled by config
        if (!config.getPiston().isEnabled()) {
            log.debug("Piston disabled by config — routing to Judge0");
            return executeWithJudge0(request);
        }

        // Gate 2: Language not supported by Piston
        LanguageRegistry.LanguageMapping mapping = languageRegistry.resolve(request.getLanguage());
        if (!mapping.isPistonSupported()) {
            log.debug("Language '{}' not supported by Piston — routing to Judge0", request.getLanguage());
            return executeWithJudge0(request);
        }

        // Gate 3: Circuit breaker is OPEN — skip Piston entirely
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("pistonEngine");
        if (cb.getState() == CircuitBreaker.State.OPEN) {
            log.warn("Circuit breaker OPEN — bypassing Piston, routing to Judge0 for language '{}'",
                    request.getLanguage());
            return executeWithJudge0(request);
        }

        // Try Piston (primary)
        try {
            ExecutionResult result = pistonService.execute(request);
            lastEngineUsed = "PISTON";
            log.debug("Piston executed successfully for language '{}' in {}ms",
                    request.getLanguage(), result.getExecutionTimeMs());
            return result;
        } catch (Exception e) {
            if (isFallbackTrigger(e)) {
                log.warn("Piston failed for language '{}' — falling back to Judge0. Reason: {} ({})",
                        request.getLanguage(), e.getClass().getSimpleName(), e.getMessage());
                return executeWithJudge0(request);
            }
            // Non-fallback exception (e.g., UnsupportedOperationException) — re-throw
            throw e;
        }
    }

    private ExecutionResult executeWithJudge0(ExecutionRequest request) {
        ExecutionResult result = judge0Service.execute(request);
        lastEngineUsed = "JUDGE0";
        return result;
    }

    /**
     * Determines whether the given exception should trigger a fallback to Judge0.
     * Matches: ConnectException, SocketTimeoutException, and HTTP 5xx (wrapped in ResourceAccessException).
     */
    private boolean isFallbackTrigger(Throwable e) {
        if (e instanceof ConnectException || e instanceof SocketTimeoutException) {
            return true;
        }
        if (e instanceof ResourceAccessException) {
            Throwable cause = e.getCause();
            return cause instanceof ConnectException || cause instanceof SocketTimeoutException;
        }
        // Resilience4j wraps exceptions — check the cause chain
        Throwable cause = e.getCause();
        if (cause != null) {
            return isFallbackTrigger(cause);
        }
        // HTTP 5xx from RestTemplate comes as HttpServerErrorException
        String className = e.getClass().getSimpleName();
        return className.contains("HttpServerError") || className.contains("ServerError");
    }

    /** Returns the last engine that handled a request. Used by the health indicator. */
    public String getLastEngineUsed() {
        return lastEngineUsed;
    }
}

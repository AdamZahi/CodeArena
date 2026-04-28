package com.codearena.execution;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom Actuator health indicator for the code execution engine.
 * Exposes circuit breaker state, last engine used, and Piston enabled flag
 * under {@code /actuator/health} as the "executionEngine" component.
 */
@Component("executionEngine")
public class ExecutionEngineHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final FallbackExecutionService fallbackService;
    private final ExecutionConfig config;

    public ExecutionEngineHealthIndicator(
            CircuitBreakerRegistry circuitBreakerRegistry,
            FallbackExecutionService fallbackService,
            ExecutionConfig config) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.fallbackService = fallbackService;
        this.config = config;
    }

    @Override
    public Health health() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("pistonEngine");
        CircuitBreaker.State state = cb.getState();
        float failureRate = cb.getMetrics().getFailureRate();

        Health.Builder builder = (state == CircuitBreaker.State.OPEN)
                ? Health.down()
                : Health.up();

        return builder
                .withDetail("circuitBreakerState", state.name())
                .withDetail("failureRate", failureRate + "%")
                .withDetail("lastEngineUsed", fallbackService.getLastEngineUsed())
                .withDetail("pistonEnabled", config.getPiston().isEnabled())
                .withDetail("pistonBaseUrl", config.getPiston().getBaseUrl())
                .withDetail("judge0BaseUrl", config.getJudge0().getBaseUrl())
                .build();
    }
}

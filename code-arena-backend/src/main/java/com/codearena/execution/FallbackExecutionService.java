package com.codearena.execution;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Primary execution orchestrator that delegates code execution to the Piston service.
 *
 * Marked {@code @Primary} so that any injection of {@link CodeExecutionService}
 * gets this orchestrator rather than the raw engine bean.
 */
@Slf4j
@Primary
@Service
public class FallbackExecutionService implements CodeExecutionService {

    private final PistonExecutionService pistonService;

    /** Tracks the last engine used — exposed via the health indicator. */
    private volatile String lastEngineUsed = "NONE";

    public FallbackExecutionService(PistonExecutionService pistonService) {
        this.pistonService = pistonService;
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        ExecutionResult result = pistonService.execute(request);
        lastEngineUsed = "PISTON";
        log.debug("Piston executed successfully for language '{}' in {}ms",
                request.getLanguage(), result.getExecutionTimeMs());
        return result;
    }

    /** Returns the last engine that handled a request. Used by the health indicator. */
    public String getLastEngineUsed() {
        return lastEngineUsed;
    }
}

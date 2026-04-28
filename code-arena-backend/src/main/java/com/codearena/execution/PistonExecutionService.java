package com.codearena.execution;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Primary code execution engine using self-hosted Piston v2.
 * Piston executes code synchronously — no polling needed.
 *
 * Protected by a Resilience4j circuit breaker ({@code pistonEngine}).
 * When the circuit opens, the FallbackExecutionService routes directly to Judge0.
 */
@Slf4j
@Service
public class PistonExecutionService implements CodeExecutionService {

    private final RestTemplate restTemplate;
    private final ExecutionConfig config;
    private final LanguageRegistry languageRegistry;

    public PistonExecutionService(
            @Qualifier("executionRestTemplate") RestTemplate restTemplate,
            ExecutionConfig config,
            LanguageRegistry languageRegistry) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.languageRegistry = languageRegistry;
    }

    @Override
    @CircuitBreaker(name = "pistonEngine")
    @SuppressWarnings("unchecked")
    public ExecutionResult execute(ExecutionRequest request) {
        long startTime = System.currentTimeMillis();

        LanguageRegistry.LanguageMapping mapping = languageRegistry.resolve(request.getLanguage());
        if (!mapping.isPistonSupported()) {
            throw new UnsupportedOperationException(
                    "Language '" + request.getLanguage() + "' is not supported by Piston");
        }

        String url = config.getPiston().getBaseUrl() + "/execute";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> fileEntry = Map.of(
                "name", mapping.fileName(),
                "content", request.getSourceCode()
        );

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("language", mapping.pistonLanguage());
        body.put("version", mapping.pistonVersion());
        body.put("files", List.of(fileEntry));
        body.put("run_timeout", config.getPiston().getRunTimeoutMs());
        body.put("compile_timeout", config.getPiston().getCompileTimeoutMs());

        if (request.getStdin() != null && !request.getStdin().isBlank()) {
            body.put("stdin", request.getStdin());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.debug("Piston execute request: language={}, version={}", mapping.pistonLanguage(), mapping.pistonVersion());

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        long elapsed = System.currentTimeMillis() - startTime;

        if (response.getBody() == null) {
            throw new RuntimeException("Piston returned empty response body");
        }

        return mapPistonResponse(response.getBody(), elapsed);
    }

    @SuppressWarnings("unchecked")
    private ExecutionResult mapPistonResponse(Map<String, Object> body, long elapsedMs) {
        Map<String, Object> run = (Map<String, Object>) body.get("run");
        Map<String, Object> compile = (Map<String, Object>) body.get("compile");

        String stdout = "";
        String stderr = "";
        int exitCode = 1; // default to failure
        String compileError = null;

        // Check for compile errors first
        if (compile != null) {
            String compileStderr = safeString(compile.get("stderr"));
            int compileCode = safeInt(compile.get("code"));
            if (compileCode != 0 || !compileStderr.isEmpty()) {
                compileError = compileStderr;
            }
        }

        // Extract run results
        if (run != null) {
            stdout = safeString(run.get("stdout"));
            stderr = safeString(run.get("stderr"));
            exitCode = safeInt(run.get("code"));
        } else if (compileError != null) {
            // Compilation failed — no run phase
            exitCode = 1;
        }

        return ExecutionResult.builder()
                .stdout(stdout.trim())
                .stderr(stderr.trim())
                .exitCode(exitCode)
                .compileError(compileError)
                .engineUsed("PISTON")
                .executionTimeMs(elapsedMs)
                .build();
    }

    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }

    private int safeInt(Object value) {
        if (value == null) return 1;
        if (value instanceof Number num) return num.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}

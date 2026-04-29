package com.codearena.module2_battle.service;

import com.codearena.module2_battle.dto.PistonExecutionRequest;
import com.codearena.module2_battle.dto.PistonExecutionResult;
import com.codearena.module2_battle.exception.CodeExecutionUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for the self-hosted Piston code execution engine.
 * Unlike Judge0, Piston executes synchronously — a single POST returns the full result.
 * No tokens, no polling, no base64 encoding.
 */
@Slf4j
@Service
public class PistonClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PistonClient(@Value("${piston.base-url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    /**
     * Executes code on Piston and returns the result synchronously.
     * POST /api/v2/execute
     */
    @SuppressWarnings("unchecked")
    public PistonExecutionResult execute(PistonExecutionRequest request) {
        try {
            String url = baseUrl + "/api/v2/execute";

            Map<String, Object> body = new HashMap<>();
            body.put("language", request.getLanguage());
            body.put("version", request.getVersion());

            Map<String, Object> file = new HashMap<>();
            file.put("content", request.getSourceCode());
            if (request.getFileName() != null && !request.getFileName().isBlank()) {
                file.put("name", request.getFileName());
            }
            body.put("files", List.of(file));
            if (request.getStdin() != null && !request.getStdin().isEmpty()) {
                body.put("stdin", request.getStdin());
            }
            body.put("run_timeout", request.getRunTimeoutMs());
            body.put("run_memory_limit", request.getMemoryLimitBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() == null) {
                throw new CodeExecutionUnavailableException(
                        new RuntimeException("Empty response from Piston"));
            }

            Map<String, Object> responseBody = response.getBody();

            // Check for top-level error message (e.g., invalid language/version)
            if (responseBody.containsKey("message")) {
                return PistonExecutionResult.builder()
                        .exitCode(1)
                        .stdout("")
                        .stderr("")
                        .compileOutput((String) responseBody.get("message"))
                        .errorMessage((String) responseBody.get("message"))
                        .build();
            }

            // Parse the compile stage (if present)
            Map<String, Object> compile = (Map<String, Object>) responseBody.get("compile");
            String compileOutput = null;
            if (compile != null) {
                String compileStderr = (String) compile.get("stderr");
                String compileStdout = (String) compile.get("output");
                int compileCode = toInt(compile.get("code"), 0);
                if (compileCode != 0) {
                    compileOutput = compileStderr != null ? compileStderr : compileStdout;
                    return PistonExecutionResult.builder()
                            .exitCode(compileCode)
                            .stdout("")
                            .stderr(compileStderr != null ? compileStderr : "")
                            .compileOutput(compileOutput)
                            .build();
                }
            }

            // Parse the run stage
            Map<String, Object> run = (Map<String, Object>) responseBody.get("run");
            if (run == null) {
                return PistonExecutionResult.builder()
                        .exitCode(1)
                        .stdout("")
                        .stderr("No run output from Piston")
                        .build();
            }

            return PistonExecutionResult.builder()
                    .exitCode(toInt(run.get("code"), 1))
                    .stdout(strOrEmpty(run.get("stdout")))
                    .stderr(strOrEmpty(run.get("stderr")))
                    .compileOutput(compileOutput)
                    .signal((String) run.get("signal"))
                    .cpuTimeMs(toIntOrNull(run.get("cpu_time")))
                    .wallTimeMs(toIntOrNull(run.get("wall_time")))
                    .memoryBytes(toLongOrNull(run.get("memory")))
                    .build();

        } catch (RestClientException e) {
            log.error("Failed to execute code on Piston: {}", e.getMessage());
            throw new CodeExecutionUnavailableException(e);
        }
    }

    private static int toInt(Object value, int defaultValue) {
        if (value instanceof Number) return ((Number) value).intValue();
        return defaultValue;
    }

    private static Integer toIntOrNull(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }

    private static Long toLongOrNull(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return null;
    }

    private static String strOrEmpty(Object value) {
        return value != null ? value.toString() : "";
    }
}

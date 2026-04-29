package com.codearena.execution;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback code execution engine using Judge0 CE (Community Edition).
 * Wraps the existing Judge0 submit → poll → decode flow into the
 * normalized {@link CodeExecutionService} interface.
 *
 * Uses {@code wait=true} to get synchronous results from Judge0 CE,
 * eliminating the need for async token polling.
 */
@Slf4j
@Service
public class Judge0ExecutionService implements CodeExecutionService {

    private final RestTemplate restTemplate;
    private final ExecutionConfig config;
    private final LanguageRegistry languageRegistry;

    public Judge0ExecutionService(
            @Qualifier("executionRestTemplate") RestTemplate restTemplate,
            ExecutionConfig config,
            LanguageRegistry languageRegistry) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.languageRegistry = languageRegistry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExecutionResult execute(ExecutionRequest request) {
        long startTime = System.currentTimeMillis();

        LanguageRegistry.LanguageMapping mapping = languageRegistry.resolve(request.getLanguage());
        int languageId = mapping.judge0Id();

        // If the registry couldn't resolve a Judge0 ID, try parsing the raw input as a numeric ID
        if (languageId <= 0) {
            try {
                languageId = Integer.parseInt(request.getLanguage());
            } catch (NumberFormatException e) {
                log.error("Cannot resolve language '{}' to a Judge0 ID", request.getLanguage());
                return ExecutionResult.builder()
                        .stdout("")
                        .stderr("Unsupported language: " + request.getLanguage())
                        .exitCode(1)
                        .engineUsed("JUDGE0")
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }
        }

        String baseUrl = config.getJudge0().getBaseUrl();
        String url = baseUrl + "/submissions?base64_encoded=true&wait=true&fields=*";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String apiKey = config.getJudge0().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("X-Auth-Token", apiKey);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("source_code", b64Encode(request.getSourceCode()));
        body.put("language_id", languageId);
        if (request.getStdin() != null && !request.getStdin().isBlank()) {
            body.put("stdin", b64Encode(request.getStdin()));
        }
        if (request.getExpectedOutput() != null && !request.getExpectedOutput().isBlank()) {
            body.put("expected_output", b64Encode(request.getExpectedOutput()));
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.debug("Judge0 execute request: languageId={}", languageId);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        long elapsed = System.currentTimeMillis() - startTime;

        if (response.getBody() == null) {
            return ExecutionResult.builder()
                    .stdout("")
                    .stderr("Judge0 returned empty response")
                    .exitCode(1)
                    .engineUsed("JUDGE0")
                    .executionTimeMs(elapsed)
                    .build();
        }

        return mapJudge0Response(response.getBody(), elapsed);
    }

    @SuppressWarnings("unchecked")
    private ExecutionResult mapJudge0Response(Map<String, Object> body, long elapsedMs) {
        String stdout = b64Decode((String) body.get("stdout"));
        String stderr = b64Decode((String) body.get("stderr"));
        String compileOutput = b64Decode((String) body.get("compile_output"));

        // Determine exit code from Judge0 status
        int exitCode = 1; // default to failure
        Map<String, Object> status = (Map<String, Object>) body.get("status");
        if (status != null) {
            int statusId = ((Number) status.get("id")).intValue();
            // Status 3 = Accepted (all tests passed)
            if (statusId == 3) {
                exitCode = 0;
            }
        }

        String compileError = (compileOutput != null && !compileOutput.isBlank()) ? compileOutput : null;

        return ExecutionResult.builder()
                .stdout(stdout.trim())
                .stderr(stderr.trim())
                .exitCode(exitCode)
                .compileError(compileError)
                .engineUsed("JUDGE0")
                .executionTimeMs(elapsedMs)
                .build();
    }

    private static String b64Encode(String value) {
        if (value == null) return "";
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    private static String b64Decode(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            return new String(Base64.getDecoder().decode(value));
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}

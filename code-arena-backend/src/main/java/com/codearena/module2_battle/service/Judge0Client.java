package com.codearena.module2_battle.service;

import com.codearena.module2_battle.dto.Judge0SubmissionRequest;
import com.codearena.module2_battle.dto.Judge0SubmissionResult;
import com.codearena.module2_battle.exception.Judge0UnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for the Judge0 code execution engine.
 * Submits code asynchronously and polls for results.
 */
@Slf4j
@Service
public class Judge0Client {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;

    public Judge0Client(
            @Value("${judge0.base-url}") String baseUrl,
            @Value("${judge0.api-key:}") String apiKey,
            @Value("${judge0.timeout-seconds:10}") int timeoutSeconds) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Submits code for execution and returns a token for polling.
     * POST /submissions?base64_encoded=false&wait=false
     */
    public String submitCode(Judge0SubmissionRequest request) {
        try {
            String url = baseUrl + "/submissions?base64_encoded=true&wait=false";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("X-Auth-Token", apiKey);
            }

            // Base64-encode all text fields to avoid newline/whitespace mangling
            Map<String, Object> body = new HashMap<>();
            body.put("language_id", request.getLanguageId());
            body.put("source_code", b64(request.getSourceCode()));
            if (request.getStdin() != null && !request.getStdin().isBlank()) {
                body.put("stdin", b64(request.getStdin()));
            }
            if (request.getExpectedOutput() != null && !request.getExpectedOutput().isBlank()) {
                body.put("expected_output", b64(request.getExpectedOutput()));
            }
            body.put("cpu_time_limit", request.getTimeLimitSeconds());
            body.put("memory_limit", request.getMemoryLimitKb());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() == null || !response.getBody().containsKey("token")) {
                throw new Judge0UnavailableException(new RuntimeException("No token in Judge0 response"));
            }

            return (String) response.getBody().get("token");
        } catch (RestClientException e) {
            log.error("Failed to submit code to Judge0: {}", e.getMessage());
            throw new Judge0UnavailableException(e);
        }
    }

    private static String b64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    /**
     * Polls a submission result by token.
     * Uses base64_encoded=true so text fields are Base64-encoded in the response,
     * then decodes them to avoid encoding/whitespace issues.
     */
    public Judge0SubmissionResult getResult(String token) {
        try {
            String url = baseUrl + "/submissions/" + token
                    + "?base64_encoded=true&fields=token,status,stdout,stderr,time,memory,compile_output";

            HttpHeaders headers = new HttpHeaders();
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("X-Auth-Token", apiKey);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Judge0SubmissionResult> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, Judge0SubmissionResult.class);

            Judge0SubmissionResult result = response.getBody();
            if (result != null) {
                result.setStdout(b64Decode(result.getStdout()));
                result.setStderr(b64Decode(result.getStderr()));
                result.setCompileOutput(b64Decode(result.getCompileOutput()));
            }
            return result;
        } catch (RestClientException e) {
            log.error("Failed to poll Judge0 result for token {}: {}", token, e.getMessage());
            throw new Judge0UnavailableException(e);
        }
    }

    private static String b64Decode(String value) {
        if (value == null || value.isBlank()) return value;
        try {
            return new String(Base64.getDecoder().decode(value));
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
}

package com.codearena.module1_challenge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class Judge0Service {

    private final String baseUrl = "https://ce.judge0.com/submissions";
    private final RestTemplate restTemplate = new RestTemplate();

    public String submit(String sourceCode, String languageId, String expectedOutput, String stdin) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("source_code", Base64.getEncoder().encodeToString(sourceCode.getBytes()));
        body.put("language_id", Integer.parseInt(languageId));
        if (expectedOutput != null && !expectedOutput.isBlank()) {
            body.put("expected_output", Base64.getEncoder().encodeToString(expectedOutput.getBytes()));
        }
        if (stdin != null && !stdin.isBlank()) {
            body.put("stdin", Base64.getEncoder().encodeToString(stdin.getBytes()));
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "?base64_encoded=true&wait=false", entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("token");
            }
        } catch (Exception e) {
            log.error("Failed to submit to Judge0", e);
        }
        return null;
    }

    public Map<String, Object> getSubmissionStatus(String token) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/" + token + "?base64_encoded=true&fields=*", Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to get status from Judge0 for token: {}", token, e);
        }
        return null;
    }

    public String decodeBase64(String encoded) {
        if (encoded == null) return "";
        try {
            return new String(Base64.getDecoder().decode(encoded));
        } catch (Exception e) {
            return encoded;
        }
    }
}

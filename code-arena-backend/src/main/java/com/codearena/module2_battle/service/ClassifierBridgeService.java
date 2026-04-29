package com.codearena.module2_battle.service;

import com.codearena.module2_battle.config.ClassifierProperties;
import com.codearena.module2_battle.dto.ComplexityClassificationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP bridge to the Python complexity-classifier microservice (FastAPI at
 * {@code battle.classifier.base-url}).
 *
 * <p>The classifier predicts a Big-O class (O(1), O(log n), O(n), O(n log n),
 * O(n^2), O(2^n)) for a code submission and converts it to a 0–100 score that
 * the scoring pipeline can combine with the runtime-based ranker score.
 * Submissions must keep resolving even when the classifier is offline, so
 * every external call falls back to a neutral placeholder result on failure
 * or when {@link ClassifierProperties#isEnabled()} is false.
 */
@Slf4j
@Service
public class ClassifierBridgeService {

    private final ClassifierProperties config;
    private final RestTemplate restTemplate;

    public ClassifierBridgeService(ClassifierProperties config) {
        this.config = config;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getConnectTimeoutMs());
        factory.setReadTimeout(config.getReadTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Classify a single submission via {@code POST /classify}. Returns a
     * fallback result when the classifier is disabled, unreachable, or
     * returns a transport error. The returned object always populates
     * {@code score} so callers can persist or broadcast it without null
     * checks.
     */
    public ComplexityClassificationResult classify(String sourceCode, String language) {
        if (!config.isEnabled()) {
            return fallback("classifier disabled by config");
        }
        if (sourceCode == null || sourceCode.isBlank()) {
            return fallback("empty source code");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("source_code", sourceCode);
            if (language != null) {
                body.put("language", language.toLowerCase());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    config.getBaseUrl() + "/classify", entity, Map.class);

            if (response == null) {
                return fallback("empty response from classifier");
            }

            String label = response.get("label") != null ? String.valueOf(response.get("label")) : null;
            String display = response.get("display") != null ? String.valueOf(response.get("display")) : null;
            double score = toDouble(response.get("score"), 0.0);
            double confidence = toDouble(response.get("confidence"), 0.0);
            String error = response.get("error") != null ? String.valueOf(response.get("error")) : null;

            return ComplexityClassificationResult.builder()
                    .label(label)
                    .display(display)
                    .score(clamp(score, 0.0, 100.0))
                    .confidence(clamp(confidence, 0.0, 1.0))
                    .error(error)
                    .fallback(false)
                    .build();

        } catch (RestClientException ex) {
            log.warn("Classifier /classify unreachable ({}): {} — using fallback",
                    config.getBaseUrl(), ex.getMessage());
            return fallback(ex.getMessage());
        } catch (Exception ex) {
            log.warn("Classifier /classify failed unexpectedly: {} — using fallback", ex.getMessage());
            return fallback(ex.getMessage());
        }
    }

    /**
     * Probes {@code GET /health}. Returns true only when the classifier
     * reports {@code model_ready=true}. Used at startup so operators see
     * early whether complexity scoring is active.
     */
    public boolean isHealthy() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(
                    config.getBaseUrl() + "/health", Map.class);
            return body != null && Boolean.TRUE.equals(body.get("model_ready"));
        } catch (Exception ex) {
            log.debug("Classifier health check failed: {}", ex.getMessage());
            return false;
        }
    }

    private ComplexityClassificationResult fallback(String reason) {
        return ComplexityClassificationResult.builder()
                .label(null)
                .display(null)
                .score(0.0)
                .confidence(0.0)
                .error(reason)
                .fallback(true)
                .build();
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }

    private static double toDouble(Object value, double defaultValue) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) { }
        }
        return defaultValue;
    }
}

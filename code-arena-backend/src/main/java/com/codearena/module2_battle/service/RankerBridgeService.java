package com.codearena.module2_battle.service;

import com.codearena.module2_battle.config.RankerProperties;
import com.codearena.module2_battle.dto.PistonExecutionResult;
import com.codearena.module2_battle.dto.RankerScoreResult;
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
 * HTTP bridge to the Python ranker microservice (FastAPI at
 * {@code battle.ranker.base-url}).
 *
 * <p>The ranker awards an optimization score (0–100) for a submission given
 * its source code, language, and Piston execution result. Battles must keep
 * resolving even when the ranker is offline, so every external call falls
 * back to a deterministic time-based estimate on failure or when
 * {@link RankerProperties#isEnabled()} is false.
 */
@Slf4j
@Service
public class RankerBridgeService {

    private static final double FALLBACK_TIME_BUDGET_MS = 5000.0;

    private final RankerProperties config;
    private final RestTemplate restTemplate;

    public RankerBridgeService(RankerProperties config) {
        this.config = config;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getConnectTimeoutMs());
        factory.setReadTimeout(config.getReadTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Score a single submission via {@code POST /score}. Returns a fallback
     * result when the ranker is disabled, unreachable, or returns a transport
     * error. The returned object always populates {@code score} so callers can
     * persist or broadcast it without null checks.
     */
    public RankerScoreResult score(String sourceCode,
                                   String language,
                                   PistonExecutionResult execution,
                                   int totalTests,
                                   int passedTests) {
        double timeMs = execution != null && execution.getCpuTimeMs() != null
                ? execution.getCpuTimeMs() : 0.0;
        double memoryKb = execution != null && execution.getMemoryKb() != null
                ? execution.getMemoryKb() : 0.0;
        int exitCode = execution != null ? execution.getExitCode() : 1;

        if (!config.isEnabled()) {
            return fallback(timeMs, memoryKb, totalTests, passedTests,
                    "ranker disabled by config");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("source_code", sourceCode != null ? sourceCode : "");
            body.put("language", normalizeLanguage(language));
            body.put("time_ms", timeMs);
            body.put("memory_kb", memoryKb);
            body.put("exit_code", exitCode);
            body.put("total_tests", Math.max(totalTests, 1));
            body.put("passed_tests", Math.max(passedTests, 0));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    config.getBaseUrl() + "/score", entity, Map.class);

            if (response == null) {
                return fallback(timeMs, memoryKb, totalTests, passedTests,
                        "empty response from ranker");
            }

            double score = toDouble(response.get("score"), 0.0);
            String error = response.get("error") != null
                    ? String.valueOf(response.get("error")) : null;

            @SuppressWarnings("unchecked")
            Map<String, Object> breakdown =
                    (Map<String, Object>) response.getOrDefault("breakdown", Map.of());

            return RankerScoreResult.builder()
                    .score(clamp(score))
                    .timeMs(toDouble(breakdown.get("time_ms"), timeMs))
                    .memoryKb(toDouble(breakdown.get("memory_kb"), memoryKb))
                    .testsRatio(toDouble(breakdown.get("tests_ratio"),
                            ratio(passedTests, totalTests)))
                    .error(error)
                    .fallback(false)
                    .build();

        } catch (RestClientException ex) {
            log.warn("Ranker /score unreachable ({}): {} — using time-based fallback",
                    config.getBaseUrl(), ex.getMessage());
            return fallback(timeMs, memoryKb, totalTests, passedTests, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Ranker /score failed unexpectedly: {} — using time-based fallback",
                    ex.getMessage());
            return fallback(timeMs, memoryKb, totalTests, passedTests, ex.getMessage());
        }
    }

    /**
     * Probes {@code GET /health}. Returns true only when the ranker reports
     * {@code model_ready=true}. Used at startup so operators see early whether
     * we'll be running with real ML scoring or the fallback.
     */
    public boolean isHealthy() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(
                    config.getBaseUrl() + "/health", Map.class);
            return body != null && Boolean.TRUE.equals(body.get("model_ready"));
        } catch (Exception ex) {
            log.debug("Ranker health check failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Time-based fallback scorer — used whenever the ranker is unreachable.
     * Maps {@code timeMs} into [0, 100] linearly against a 5s budget; failed
     * test cases are penalized proportionally so a wrong answer cannot beat
     * a correct one.
     */
    private RankerScoreResult fallback(double timeMs, double memoryKb,
                                       int totalTests, int passedTests, String reason) {
        double timeScore = 100.0 - (timeMs / FALLBACK_TIME_BUDGET_MS) * 100.0;
        double testRatio = ratio(passedTests, totalTests);
        double score = clamp(timeScore) * testRatio;

        return RankerScoreResult.builder()
                .score(round2(score))
                .timeMs(timeMs)
                .memoryKb(memoryKb)
                .testsRatio(testRatio)
                .error(reason)
                .fallback(true)
                .build();
    }

    private static double ratio(int passed, int total) {
        if (total <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, (double) passed / total));
    }

    private static double clamp(double v) {
        if (Double.isNaN(v)) return 0.0;
        return Math.max(0.0, Math.min(100.0, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double toDouble(Object value, double defaultValue) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) { }
        }
        return defaultValue;
    }

    /**
     * Maps the language identifier we store on submissions to the lowercase
     * key the ranker expects (it treats "Java", "JAVA", "java" identically).
     */
    private static String normalizeLanguage(String language) {
        return language != null ? language.toLowerCase() : "unknown";
    }
}

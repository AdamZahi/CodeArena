package com.codearena.module2_battle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the Score Ranker microservice (Python FastAPI).
 * Bound from the {@code battle.ranker} section of application.yml.
 */
@Data
@Component
@ConfigurationProperties(prefix = "battle.ranker")
public class RankerProperties {

    /** Base URL of the ranker FastAPI service (e.g. http://localhost:8010). */
    private String baseUrl = "http://localhost:8010";

    /** HTTP connect timeout for ranker calls. */
    private int connectTimeoutMs = 1500;

    /** HTTP read timeout for ranker calls. */
    private int readTimeoutMs = 4000;

    /** When false, skip ranker calls entirely (force fallback scoring). */
    private boolean enabled = true;
}

package com.codearena.module2_battle.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the Complexity Classifier microservice (Python FastAPI).
 * Bound from the {@code battle.classifier} section of application.yml.
 */
@Data
@Component
@ConfigurationProperties(prefix = "battle.classifier")
public class ClassifierProperties {

    /** Base URL of the classifier FastAPI service (e.g. http://localhost:8002). */
    private String baseUrl = "http://localhost:8002";

    /** HTTP connect timeout for classifier calls. */
    private int connectTimeoutMs = 1500;

    /** HTTP read timeout for classifier calls (CodeBERT inference can be slow on CPU). */
    private int readTimeoutMs = 8000;

    /** When false, skip classifier calls entirely. */
    private boolean enabled = true;
}

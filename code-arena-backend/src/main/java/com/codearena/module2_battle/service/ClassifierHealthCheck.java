package com.codearena.module2_battle.service;

import com.codearena.module2_battle.config.ClassifierProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Probes the Complexity Classifier microservice once at startup so operators
 * see immediately whether submissions will be tagged with a Big-O label.
 * Runs asynchronously — the application never refuses to start because the
 * classifier is offline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassifierHealthCheck {

    private final ClassifierProperties config;
    private final ClassifierBridgeService classifierBridgeService;

    @PostConstruct
    @Async
    public void probe() {
        if (!config.isEnabled()) {
            log.warn("Complexity Classifier disabled by config — submissions will not carry a Big-O label");
            return;
        }
        boolean ready = classifierBridgeService.isHealthy();
        if (ready) {
            log.info("Complexity Classifier reachable at {} — model_ready=true", config.getBaseUrl());
        } else {
            log.warn("Complexity Classifier not ready at {} — submissions will skip complexity scoring",
                    config.getBaseUrl());
        }
    }
}

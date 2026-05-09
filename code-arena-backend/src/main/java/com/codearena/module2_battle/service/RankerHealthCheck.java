package com.codearena.module2_battle.service;

import com.codearena.module2_battle.config.RankerProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Probes the Score Ranker microservice once at startup so operators see
 * immediately whether battles will use real ML scoring or the time-based
 * fallback. Runs asynchronously — the application never refuses to start
 * because the ranker is offline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankerHealthCheck {

    private final RankerProperties config;
    private final RankerBridgeService rankerBridgeService;

    @PostConstruct
    @Async
    public void probe() {
        if (!config.isEnabled()) {
            log.warn("Score Ranker disabled by config — submissions will use fallback scoring");
            return;
        }
        boolean ready = rankerBridgeService.isHealthy();
        if (ready) {
            log.info("Score Ranker reachable at {} — model_ready=true", config.getBaseUrl());
        } else {
            log.warn("Score Ranker not ready at {} — fallback scoring active until it is",
                    config.getBaseUrl());
        }
    }
}

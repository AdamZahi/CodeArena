package com.codearena.module2_battle.admin.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lightweight in-memory cache for backoffice analytics queries.
 * 5-minute TTL is implemented by clearing all analytics caches on a fixed interval,
 * keeping the dependency footprint small (no Caffeine).
 */
@Configuration
@EnableCaching
public class AnalyticsCacheConfig {

    public static final List<String> ANALYTICS_CACHES = List.of(
            "battleSummary",
            "battleTimeline",
            "topChallenges",
            "topPlayers",
            "languageDistribution",
            "outcomeDistribution",
            "avgDuration"
    );

    @Primary
    @Bean
    public CacheManager analyticsCacheManager() {
        ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager();
        cm.setCacheNames(ANALYTICS_CACHES);
        return cm;
    }

    @Component
    @RequiredArgsConstructor
    static class AnalyticsCacheEvictor {

        private final CacheManager analyticsCacheManager;

        /** Evicts all backoffice analytics caches every 5 minutes. */
        @Scheduled(fixedRate = 5L * 60L * 1000L)
        public void evictAll() {
            for (String name : ANALYTICS_CACHES) {
                var cache = analyticsCacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            }
        }
    }
}

package com.codearena.module2_battle.service;

import com.codearena.module2_battle.repository.SharedResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SharedResultCleanupScheduler {

    private final SharedResultRepository sharedResultRepository;

    /**
     * Daily at 03:00 UTC, delete expired SharedResult rows.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void cleanupExpiredSharedResults() {
        int deleted = sharedResultRepository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("Deleted {} expired shared result records", deleted);
        }
    }
}

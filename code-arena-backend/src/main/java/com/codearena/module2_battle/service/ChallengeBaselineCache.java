package com.codearena.module2_battle.service;

import com.codearena.module2_battle.entity.BattleSubmission;
import com.codearena.module2_battle.repository.BattleSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for median runtime/memory of ACCEPTED submissions per challenge.
 * Used by the efficiency score formula to avoid expensive per-submission queries.
 */
@Component
@RequiredArgsConstructor
public class ChallengeBaselineCache {

    private final BattleSubmissionRepository submissionRepository;

    private final ConcurrentHashMap<String, BaselineStats> cache = new ConcurrentHashMap<>();

    public record BaselineStats(double medianRuntimeMs, double medianMemoryKb) {}

    /**
     * Returns baseline stats for a challenge, computing from DB if not cached.
     * Returns null if no ACCEPTED submissions with metrics exist for this challenge.
     */
    public BaselineStats getOrCompute(String challengeId) {
        return cache.computeIfAbsent(challengeId, this::computeFromDb);
    }

    /**
     * Called after processing each ACCEPTED submission to refresh the cache entry.
     */
    public void invalidate(String challengeId) {
        cache.remove(challengeId);
    }

    private BaselineStats computeFromDb(String challengeId) {
        List<BattleSubmission> accepted = submissionRepository
                .findAllAcceptedWithMetricsByChallengeId(challengeId);

        if (accepted.isEmpty()) {
            return null;
        }

        double medianRuntime = computeMedian(
                accepted.stream().map(BattleSubmission::getRuntimeMs).map(Integer::doubleValue).sorted().toList());
        double medianMemory = computeMedian(
                accepted.stream().map(BattleSubmission::getMemoryKb).map(Integer::doubleValue).sorted().toList());

        return new BaselineStats(medianRuntime, medianMemory);
    }

    /**
     * Computes median from a sorted list. For even-sized lists, returns the average of the two middle values.
     */
    private double computeMedian(List<Double> sorted) {
        int size = sorted.size();
        if (size == 0) return 0;
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        }
        return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    }
}

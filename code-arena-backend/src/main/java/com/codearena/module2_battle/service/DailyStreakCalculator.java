package com.codearena.module2_battle.service;

import com.codearena.module2_battle.entity.DailyChallenge;
import com.codearena.module2_battle.entity.DailyEntry;
import com.codearena.module2_battle.repository.DailyChallengeRepository;
import com.codearena.module2_battle.repository.DailyEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Computes daily challenge streaks by walking completed entry dates backward.
 *
 * Streak logic:
 * - A streak is a sequence of consecutive UTC calendar days with a COMPLETED DailyEntry.
 * - Current streak counts back from today; if today's entry is not yet completed, starts from yesterday.
 * - Any gap of one or more days breaks the streak to zero.
 */
@Component
@RequiredArgsConstructor
public class DailyStreakCalculator {

    private final DailyEntryRepository dailyEntryRepository;
    private final DailyChallengeRepository dailyChallengeRepository;

    /**
     * Computes current streak: how many consecutive days ending today (or yesterday
     * if today's challenge is not yet completed) the user has a COMPLETED entry.
     */
    public int computeCurrentStreak(String userId) {
        List<DailyEntry> completedEntries = dailyEntryRepository.findCompletedByUserOrderByDateDesc(userId);
        if (completedEntries.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int streak = 0;

        // Resolve the challenge date for each entry and walk backward
        LocalDate expectedDate = null;
        for (DailyEntry entry : completedEntries) {
            LocalDate entryDate = resolveChallengeDate(entry);
            if (entryDate == null) continue;

            if (expectedDate == null) {
                // First entry: must be today or yesterday to start a current streak
                if (entryDate.equals(today) || entryDate.equals(today.minusDays(1))) {
                    expectedDate = entryDate;
                    streak = 1;
                } else {
                    // Most recent completed entry is older than yesterday — no current streak
                    return 0;
                }
            } else {
                if (entryDate.equals(expectedDate.minusDays(1))) {
                    streak++;
                    expectedDate = entryDate;
                } else if (entryDate.equals(expectedDate)) {
                    // Duplicate date (shouldn't happen), skip
                    continue;
                } else {
                    // Gap detected — streak ends
                    break;
                }
            }
        }

        return streak;
    }

    /**
     * Computes the all-time longest streak for a user across their entire history.
     */
    public int computeLongestStreak(String userId) {
        List<DailyEntry> completedEntries = dailyEntryRepository.findCompletedByUserOrderByDateDesc(userId);
        if (completedEntries.isEmpty()) {
            return 0;
        }

        int longestStreak = 0;
        int currentStreak = 0;
        LocalDate previousDate = null;

        for (DailyEntry entry : completedEntries) {
            LocalDate entryDate = resolveChallengeDate(entry);
            if (entryDate == null) continue;

            if (previousDate == null) {
                currentStreak = 1;
                previousDate = entryDate;
            } else if (entryDate.equals(previousDate.minusDays(1))) {
                currentStreak++;
                previousDate = entryDate;
            } else if (entryDate.equals(previousDate)) {
                // Duplicate date, skip
                continue;
            } else {
                // Gap — reset streak
                longestStreak = Math.max(longestStreak, currentStreak);
                currentStreak = 1;
                previousDate = entryDate;
            }
        }

        return Math.max(longestStreak, currentStreak);
    }

    private LocalDate resolveChallengeDate(DailyEntry entry) {
        return dailyChallengeRepository.findById(
                java.util.UUID.fromString(entry.getDailyChallengeId())
        ).map(DailyChallenge::getChallengeDate).orElse(null);
    }
}

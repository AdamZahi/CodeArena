package com.codearena.module2_battle.service;

import com.codearena.module2_battle.dto.BattleStatsResponse;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.BattleSubmissionStatus;
import com.codearena.module2_battle.enums.DailyEntryStatus;
import com.codearena.module2_battle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes global battle module statistics via aggregate DB queries.
 * No records are loaded into memory — all counts use @Query aggregate methods.
 */
@Service
@RequiredArgsConstructor
public class BattleStatsService {

    private final BattleRoomRepository battleRoomRepository;
    private final BattleSubmissionRepository submissionRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final PlayerRatingRepository playerRatingRepository;

    public BattleStatsResponse getGlobalStats() {
        long totalRoomsCreated = battleRoomRepository.count();
        long totalRoomsFinished = battleRoomRepository.countByStatus(BattleRoomStatus.FINISHED);
        long totalRoomsCancelled = battleRoomRepository.countByStatus(BattleRoomStatus.CANCELLED);

        long totalSubmissions = submissionRepository.count();
        long totalAcceptedSubmissions = submissionRepository.countByStatus(BattleSubmissionStatus.ACCEPTED);
        double globalAcceptanceRate = totalSubmissions > 0
                ? (double) totalAcceptedSubmissions / totalSubmissions * 100.0
                : 0.0;

        // Submissions by language
        Map<String, Long> submissionsByLanguage = new LinkedHashMap<>();
        for (Object[] row : submissionRepository.countGroupedByLanguage()) {
            submissionsByLanguage.put((String) row[0], (Long) row[1]);
        }

        // Rooms by mode
        Map<String, Long> roomsByMode = new LinkedHashMap<>();
        for (Object[] row : battleRoomRepository.countGroupedByMode()) {
            roomsByMode.put(row[0].toString(), (Long) row[1]);
        }

        // Active daily streak players (>= 3 completed entries)
        long activeDailyStreakPlayers;
        try {
            activeDailyStreakPlayers = playerRatingRepository.countUsersWithStreakAtLeast3();
        } catch (Exception e) {
            // Fallback if subquery not supported
            activeDailyStreakPlayers = 0;
        }

        long totalDailyEntriesAllTime = dailyEntryRepository.count();

        return BattleStatsResponse.builder()
                .totalRoomsCreated(totalRoomsCreated)
                .totalRoomsFinished(totalRoomsFinished)
                .totalRoomsCancelled(totalRoomsCancelled)
                .totalSubmissions(totalSubmissions)
                .totalAcceptedSubmissions(totalAcceptedSubmissions)
                .globalAcceptanceRate(Math.round(globalAcceptanceRate * 100.0) / 100.0)
                .submissionsByLanguage(submissionsByLanguage)
                .roomsByMode(roomsByMode)
                .activeDailyStreakPlayers(activeDailyStreakPlayers)
                .totalDailyEntriesAllTime(totalDailyEntriesAllTime)
                .build();
    }
}

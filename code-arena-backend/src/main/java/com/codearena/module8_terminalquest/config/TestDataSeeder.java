package com.codearena.module8_terminalquest.config;

import com.codearena.module8_terminalquest.entity.*;
import com.codearena.module8_terminalquest.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class TestDataSeeder implements CommandLineRunner {

    private final LevelProgressRepository     levelProgressRepository;
    private final SurvivalSessionRepository   survivalSessionRepository;
    private final SurvivalLeaderboardRepository survivalLeaderboardRepository;
    private final StoryMissionRepository      storyMissionRepository;
    private final ActivityLogRepository       activityLogRepository;

    private final Random rng = new Random(42);

    // ── Player profiles ───────────────────────────────────────────────────────

    private static final String ALICE   = "player-alice";
    private static final String BOB     = "player-bob";
    private static final String CHARLIE = "player-charlie";
    private static final String DIANA   = "player-diana";
    private static final String EVE     = "player-eve";

    @Override
    public void run(String... args) {
        if (levelProgressRepository.count() >= 20) {
            log.info("[test-data] Skipping seed — data already present ({} entries)", levelProgressRepository.count());
            return;
        }

        List<StoryMission> missions = storyMissionRepository.findAll();
        if (missions.isEmpty()) {
            log.warn("[test-data] No missions found — skipping seed. Run DataInitializer first.");
            return;
        }

        log.info("[test-data] Seeding test data for 5 players ({} missions available)…", missions.size());

        seedMissionProgress(missions);
        seedSurvivalData();
        seedActivityLogs();

        log.info("[test-data] Seeded test data for 5 players");
    }

    // ── Mission progress ──────────────────────────────────────────────────────

    private void seedMissionProgress(List<StoryMission> missions) {
        int total = missions.size();

        // player-alice  : first 50% of missions, good player
        seedPlayer(ALICE,   missions, 0,             total / 2,           2, 3, 1, 2);

        // player-bob    : first 25%, average player
        seedPlayer(BOB,     missions, 0,             Math.max(1, total / 4), 1, 2, 3, 4);

        // player-charlie: all missions, expert
        seedPlayer(CHARLIE, missions, 0,             total,               3, 3, 1, 1);

        // player-diana  : first 75%, average-good
        seedPlayer(DIANA,   missions, 0,             total * 3 / 4,       2, 2, 2, 3);

        // player-eve    : first 2 missions only, beginner
        seedPlayer(EVE,     missions, 0,             Math.min(2, total),  1, 1, 5, 6);
    }

    private void seedPlayer(String userId, List<StoryMission> missions,
                            int from, int to,
                            int minStars, int maxStars,
                            int minAttempts, int maxAttempts) {
        for (int i = from; i < to && i < missions.size(); i++) {
            StoryMission mission = missions.get(i);
            int attempts = minAttempts + rng.nextInt(Math.max(1, maxAttempts - minAttempts + 1));
            int stars    = minStars    + rng.nextInt(Math.max(1, maxStars    - minStars    + 1));

            levelProgressRepository.save(LevelProgress.builder()
                    .userId(userId)
                    .mission(mission)
                    .completed(true)
                    .starsEarned(Math.min(stars, 3))
                    .attempts(attempts)
                    .completedAt(Instant.now().minus(rng.nextInt(30), ChronoUnit.DAYS).toString())
                    .build());
        }
        log.info("[test-data] {} — {} mission(s) seeded", userId, Math.min(to, missions.size()) - from);
    }

    // ── Survival data ─────────────────────────────────────────────────────────

    private void seedSurvivalData() {
        seedSurvival(ALICE,   5,  1200);
        seedSurvival(CHARLIE, 12, 4500);
        seedSurvival(DIANA,   8,  2800);
    }

    private void seedSurvival(String userId, int wave, int score) {
        String started = Instant.now().minus(rng.nextInt(14) + 1, ChronoUnit.DAYS).toString();
        String ended   = Instant.now().minus(rng.nextInt(14),     ChronoUnit.HOURS).toString();

        survivalSessionRepository.save(SurvivalSession.builder()
                .userId(userId)
                .waveReached(wave)
                .score(score)
                .livesRemaining(0)
                .startedAt(started)
                .endedAt(ended)
                .build());

        SurvivalLeaderboard board = survivalLeaderboardRepository.findByUserId(userId)
                .orElseGet(() -> SurvivalLeaderboard.builder().userId(userId).bestWave(0).bestScore(0).build());
        if (wave > board.getBestWave() || (wave == board.getBestWave() && score > board.getBestScore())) {
            board.setBestWave(wave);
            board.setBestScore(score);
            survivalLeaderboardRepository.save(board);
        }

        log.info("[test-data] {} — survival wave={} score={} seeded", userId, wave, score);
    }

    // ── Activity logs ─────────────────────────────────────────────────────────

    private void seedActivityLogs() {
        // activity counts per player
        seedLogs(ALICE,   12);
        seedLogs(BOB,      7);
        seedLogs(CHARLIE, 15);
        seedLogs(DIANA,   10);
        seedLogs(EVE,      5);
    }

    private static final ActivityType[] ACTIVITY_POOL = {
        ActivityType.MISSION_COMPLETED,
        ActivityType.MISSION_COMPLETED,
        ActivityType.MISSION_FAILED,
        ActivityType.SURVIVAL_STARTED,
        ActivityType.SURVIVAL_ENDED,
        ActivityType.HINT_USED,
        ActivityType.LEVEL_COMPLETED,
        ActivityType.LEVEL_FAILED,
    };

    private void seedLogs(String userId, int count) {
        for (int i = 0; i < count; i++) {
            ActivityType type = ACTIVITY_POOL[rng.nextInt(ACTIVITY_POOL.length)];
            int daysAgo = rng.nextInt(30);

            // We use a fake createdAt by saving a normal log — JPA sets createdAt to NOW,
            // so for demo purposes we just spread them randomly and let the DB clock apply.
            activityLogRepository.save(ActivityLog.builder()
                    .userId(userId)
                    .activityType(type)
                    .metadata("{\"seeded\":true,\"daysAgo\":" + daysAgo + "}")
                    .build());
        }
        log.info("[test-data] {} — {} activity log(s) seeded", userId, count);
    }
}

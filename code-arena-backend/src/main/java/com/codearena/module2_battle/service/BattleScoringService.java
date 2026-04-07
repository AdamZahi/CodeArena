package com.codearena.module2_battle.service;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module2_battle.config.TimeLimitProperties;
import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.entity.*;
import com.codearena.module2_battle.enums.*;
import com.codearena.module2_battle.exception.ActiveSeasonNotFoundException;
import com.codearena.module2_battle.exception.BattleRoomNotFoundException;
import com.codearena.module2_battle.repository.*;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrator for the entire post-match scoring pipeline.
 * Triggered once after BattleRoomStatus becomes FINISHED.
 *
 * Scoring formula (per challenge, max 950):
 *   correctnessScore (500) + speedScore (300) + efficiencyScore (150) - attemptPenalty (max 50)
 *
 * Total score = sum of per-challenge scores across all challenges.
 * Max possible score = 950 × room.challengeCount.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleScoringService {

    private final BattleRoomRepository battleRoomRepository;
    private final BattleParticipantRepository participantRepository;
    private final BattleRoomChallengeRepository roomChallengeRepository;
    private final BattleSubmissionRepository submissionRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final SeasonRepository seasonRepository;
    private final DailyChallengeRepository dailyChallengeRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final ChallengeBaselineCache baselineCache;
    private final BadgeEvaluationService badgeEvaluationService;
    private final TimeLimitProperties timeLimitProperties;

    private static final int K_FACTOR = 32;
    private static final int MIN_ELO = 100;
    private static final int MAX_CORRECTNESS = 500;
    private static final int MAX_SPEED = 300;
    private static final int MAX_EFFICIENCY = 150;
    private static final int MAX_PENALTY = 50;
    private static final int PENALTY_PER_ATTEMPT = 10;

    // ──────────────────────────────────────────────
    // 4.1 — processMatchResults
    // ──────────────────────────────────────────────

    /**
     * Computes scores, ranks, ELO, and badges for a finished match.
     * Idempotent: if final_score is already set on any participant, returns existing results.
     */
    @Transactional
    public PostMatchSummaryResponse processMatchResults(String roomId) {
        BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId))
                .orElseThrow(() -> new BattleRoomNotFoundException(roomId));

        List<BattleParticipant> players = participantRepository
                .findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);

        // Idempotency guard: if scoring has already run, return cached results
        boolean alreadyScored = players.stream().anyMatch(p -> p.getScore() != null);
        if (alreadyScored) {
            log.info("Scoring already processed for room {} — returning existing results", roomId);
            return buildPostMatchSummary(room, players);
        }

        List<BattleRoomChallenge> roomChallenges = roomChallengeRepository
                .findByRoomIdOrderByPositionAsc(roomId);

        int matchDurationSeconds = getMatchDurationSeconds(room.getMode());

        // Step 1-2: Compute final_score for each participant
        Map<String, List<ScoreBreakdownResponse>> breakdownMap = new HashMap<>();
        for (BattleParticipant player : players) {
            String participantId = player.getId().toString();
            List<BattleSubmission> submissions = submissionRepository
                    .findByParticipantIdOrderBySubmittedAtAsc(participantId);

            List<ScoreBreakdownResponse> breakdowns = new ArrayList<>();
            int totalScore = 0;

            for (BattleRoomChallenge rc : roomChallenges) {
                ScoreBreakdownResponse breakdown = computeChallengeScore(
                        rc, submissions, room.getStartsAt(), matchDurationSeconds);
                breakdowns.add(breakdown);
                totalScore += breakdown.getTotalChallengeScore();

                // Invalidate baseline cache for accepted challenges
                if (breakdown.isSolved()) {
                    baselineCache.invalidate(rc.getChallengeId());
                }
            }

            player.setScore(totalScore);
            breakdownMap.put(participantId, breakdowns);
        }

        // Step 3: Assign final_rank using competition ranking
        assignFinalRanks(players, room);

        // Step 4: Persist final_score and final_rank
        participantRepository.saveAll(players);

        // Step 5: ELO updates for ranked modes
        Map<String, Integer> newEloMap = new HashMap<>();
        Map<String, String> newTierMap = new HashMap<>();
        Map<String, Integer> eloBeforeMap = new HashMap<>();
        Map<String, String> tierBeforeMap = new HashMap<>();
        if (isRankedMode(room.getMode())) {
            computeAndPersistElo(players, room, newEloMap, newTierMap, eloBeforeMap, tierBeforeMap);
        }

        // Step 6: Badge evaluation
        Map<String, List<String>> badgesMap = badgeEvaluationService.evaluateAndAward(roomId, room, players);

        // Step 7: Daily entry updates
        if (room.getMode() == BattleMode.DAILY) {
            updateDailyEntries(roomId, room, players);
        }

        // Step 8: Build response
        return buildPostMatchSummaryWithDetails(room, players, breakdownMap, newEloMap, newTierMap, eloBeforeMap, tierBeforeMap, badgesMap);
    }

    // ──────────────────────────────────────────────
    // Score computation
    // ──────────────────────────────────────────────

    /**
     * Computes the score breakdown for a single challenge for a single player.
     *
     * Worked example (with real numbers):
     *   Player solved challenge in 180s out of 1800s match, runtime 50ms (baseline 100ms),
     *   memory 5000kb (baseline 8000kb), 2 failed attempts before accepting:
     *   - correctnessScore = 500
     *   - speedScore = round(300 × max(0, 1 - 180/1800)) = round(300 × 0.9) = 270
     *   - runtimeRatio = min(50/100, 2.0) = 0.5, memoryRatio = min(5000/8000, 2.0) = 0.625
     *   - efficiencyScore = round(150 × (1 - (0.5 + 0.625)/4)) = round(150 × 0.71875) = 108
     *   - attemptPenalty = min(2 × 10, 50) = 20
     *   - total = max(0, 500 + 270 + 108 - 20) = 858
     */
    private ScoreBreakdownResponse computeChallengeScore(
            BattleRoomChallenge rc, List<BattleSubmission> allSubmissions,
            LocalDateTime startsAt, int matchDurationSeconds) {

        String roomChallengeId = rc.getId().toString();
        List<BattleSubmission> challengeSubmissions = allSubmissions.stream()
                .filter(s -> s.getRoomChallengeId().equals(roomChallengeId))
                .toList();

        BattleSubmission acceptedSub = challengeSubmissions.stream()
                .filter(s -> s.getStatus() == BattleSubmissionStatus.ACCEPTED)
                .findFirst().orElse(null);

        boolean solved = acceptedSub != null;
        int attemptCount = challengeSubmissions.size();

        // Resolve challenge title
        String challengeTitle = "";
        Challenge challenge = challengeRepository.findById(Long.parseLong(rc.getChallengeId())).orElse(null);
        if (challenge != null) {
            challengeTitle = challenge.getTitle();
        }

        if (!solved) {
            int failedAttempts = attemptCount;
            int penalty = Math.min(failedAttempts * PENALTY_PER_ATTEMPT, MAX_PENALTY);
            return ScoreBreakdownResponse.builder()
                    .roomChallengeId(roomChallengeId)
                    .challengePosition(rc.getPosition())
                    .challengeTitle(challengeTitle)
                    .solved(false)
                    .correctnessScore(0)
                    .speedScore(0)
                    .efficiencyScore(0)
                    .attemptPenalty(penalty)
                    .totalChallengeScore(0)
                    .attemptCount(attemptCount)
                    .bestRuntimeMs(null)
                    .bestMemoryKb(null)
                    .solvedInSeconds(-1)
                    .build();
        }

        // Correctness: 500 points for solving
        int correctnessScore = MAX_CORRECTNESS;

        // Speed score: linear decay based on time taken vs match duration
        long solvedInSeconds = Duration.between(startsAt, acceptedSub.getSubmittedAt()).getSeconds();
        int speedScore;
        if (matchDurationSeconds <= 0) {
            // No time limit (PRACTICE/DAILY) — flat half-value
            speedScore = 150;
        } else {
            double ratio = 1.0 - ((double) solvedInSeconds / matchDurationSeconds);
            speedScore = (int) Math.round(MAX_SPEED * Math.max(0, ratio));
        }

        // Efficiency score: based on runtime/memory vs challenge baseline
        Integer runtimeMs = acceptedSub.getRuntimeMs();
        Integer memoryKb = acceptedSub.getMemoryKb();
        int efficiencyScore;

        if (runtimeMs == null || memoryKb == null) {
            efficiencyScore = 75; // flat half-value when metrics unavailable
        } else {
            ChallengeBaselineCache.BaselineStats baseline = baselineCache.getOrCompute(rc.getChallengeId());
            if (baseline == null) {
                // First submission globally — no baseline yet
                efficiencyScore = 75;
            } else {
                double runtimeRatio = Math.min((double) runtimeMs / baseline.medianRuntimeMs(), 2.0);
                double memoryRatio = Math.min((double) memoryKb / baseline.medianMemoryKb(), 2.0);
                efficiencyScore = (int) Math.round(MAX_EFFICIENCY * (1.0 - (runtimeRatio + memoryRatio) / 4.0));
                efficiencyScore = Math.max(0, efficiencyScore);
            }
        }

        // Attempt penalty: 10 per failed attempt, capped at 50
        int failedAttempts = attemptCount - 1; // subtract the accepted one
        int attemptPenalty = Math.min(failedAttempts * PENALTY_PER_ATTEMPT, MAX_PENALTY);

        int totalChallengeScore = Math.max(0, correctnessScore + speedScore + efficiencyScore - attemptPenalty);

        return ScoreBreakdownResponse.builder()
                .roomChallengeId(roomChallengeId)
                .challengePosition(rc.getPosition())
                .challengeTitle(challengeTitle)
                .solved(true)
                .correctnessScore(correctnessScore)
                .speedScore(speedScore)
                .efficiencyScore(efficiencyScore)
                .attemptPenalty(attemptPenalty)
                .totalChallengeScore(totalChallengeScore)
                .attemptCount(attemptCount)
                .bestRuntimeMs(runtimeMs)
                .bestMemoryKb(memoryKb)
                .solvedInSeconds(solvedInSeconds)
                .build();
    }

    // ──────────────────────────────────────────────
    // Rank assignment
    // ──────────────────────────────────────────────

    /**
     * Assigns final_rank using competition ranking (1224 style):
     * 1. Sort by final_score DESC
     * 2. Tie-break by total accepted submission time ASC
     * 3. Tie-break by total attempts ASC
     * 4. Same rank for genuine ties, skip next rank number
     */
    private void assignFinalRanks(List<BattleParticipant> players, BattleRoom room) {
        // Pre-compute tie-breaker values
        Map<String, Long> totalAcceptedTime = new HashMap<>();
        Map<String, Integer> totalAttempts = new HashMap<>();

        for (BattleParticipant player : players) {
            String pid = player.getId().toString();
            List<BattleSubmission> subs = submissionRepository.findByParticipantIdOrderBySubmittedAtAsc(pid);

            long acceptedTimeSum = subs.stream()
                    .filter(s -> s.getStatus() == BattleSubmissionStatus.ACCEPTED)
                    .mapToLong(s -> Duration.between(room.getStartsAt(), s.getSubmittedAt()).getSeconds())
                    .sum();
            totalAcceptedTime.put(pid, acceptedTimeSum);
            totalAttempts.put(pid, subs.size());
        }

        List<BattleParticipant> sorted = new ArrayList<>(players);
        sorted.sort(Comparator
                .comparingInt((BattleParticipant p) -> p.getScore() != null ? p.getScore() : 0).reversed()
                .thenComparingLong(p -> totalAcceptedTime.getOrDefault(p.getId().toString(), 0L))
                .thenComparingInt(p -> totalAttempts.getOrDefault(p.getId().toString(), 0)));

        int rank = 1;
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                BattleParticipant prev = sorted.get(i - 1);
                BattleParticipant curr = sorted.get(i);
                String prevId = prev.getId().toString();
                String currId = curr.getId().toString();

                boolean sameScore = Objects.equals(prev.getScore(), curr.getScore());
                boolean sameTime = Objects.equals(totalAcceptedTime.get(prevId), totalAcceptedTime.get(currId));
                boolean sameAttempts = Objects.equals(totalAttempts.get(prevId), totalAttempts.get(currId));

                if (!(sameScore && sameTime && sameAttempts)) {
                    rank = i + 1; // Skip ranks for genuine ties (standard competition ranking)
                }
            }
            sorted.get(i).setRank(rank);
        }
    }

    // ──────────────────────────────────────────────
    // ELO calculation
    // ──────────────────────────────────────────────

    /**
     * Computes ELO changes using pairwise comparison between all players.
     *
     * For each pair (A, B):
     *   expectedA = 1 / (1 + 10^((eloB - eloA) / 400))
     *   actualA = 1.0 if rank(A) < rank(B), 0.0 if rank(A) > rank(B), 0.5 if tied
     *   changeA += K × (actualA - expectedA)
     *
     * Total elo_change = round(sum of all pairwise changes).
     * New ELO = current ELO + elo_change, floored at 100.
     */
    private void computeAndPersistElo(List<BattleParticipant> players, BattleRoom room,
                                       Map<String, Integer> newEloMap, Map<String, String> newTierMap,
                                       Map<String, Integer> eloBeforeMap, Map<String, String> tierBeforeMap) {
        Optional<Season> activeSeasonOpt = seasonRepository.findFirstByIsActiveTrue();
        if (activeSeasonOpt.isEmpty()) {
            log.warn("No active season found — skipping ELO updates for room {}", room.getId());
            return;
        }
        Season activeSeason = activeSeasonOpt.get();
        String seasonId = activeSeason.getId().toString();

        // Load or create PlayerRating for each player
        Map<String, PlayerRating> ratings = new HashMap<>();
        for (BattleParticipant player : players) {
            PlayerRating rating = playerRatingRepository
                    .findByUserIdAndSeasonId(player.getUserId(), seasonId)
                    .orElseGet(() -> PlayerRating.builder()
                            .userId(player.getUserId())
                            .seasonId(seasonId)
                            .elo(1000)
                            .tier(PlayerTier.BRONZE)
                            .wins(0).losses(0).draws(0).winStreak(0)
                            .build());
            ratings.put(player.getId().toString(), rating);
        }

        // Pairwise ELO computation
        Map<String, Double> eloChanges = new HashMap<>();
        for (BattleParticipant p : players) {
            eloChanges.put(p.getId().toString(), 0.0);
        }

        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                BattleParticipant pA = players.get(i);
                BattleParticipant pB = players.get(j);
                String idA = pA.getId().toString();
                String idB = pB.getId().toString();

                int eloA = ratings.get(idA).getElo();
                int eloB = ratings.get(idB).getElo();

                double expectedA = 1.0 / (1.0 + Math.pow(10, (double)(eloB - eloA) / 400.0));
                double expectedB = 1.0 - expectedA;

                double actualA, actualB;
                if (pA.getRank() < pB.getRank()) {
                    actualA = 1.0; actualB = 0.0;
                } else if (pA.getRank() > pB.getRank()) {
                    actualA = 0.0; actualB = 1.0;
                } else {
                    actualA = 0.5; actualB = 0.5;
                }

                eloChanges.merge(idA, K_FACTOR * (actualA - expectedA), Double::sum);
                eloChanges.merge(idB, K_FACTOR * (actualB - expectedB), Double::sum);
            }
        }

        // Apply ELO changes, update tier, win/loss/streak
        int playerCount = players.size();
        for (BattleParticipant player : players) {
            String pid = player.getId().toString();
            int eloChange = (int) Math.round(eloChanges.get(pid));
            player.setEloChange(eloChange);

            PlayerRating rating = ratings.get(pid);
            int eloBefore = rating.getElo();
            String tierBefore = rating.getTier() != null ? rating.getTier().name() : PlayerTier.BRONZE.name();
            eloBeforeMap.put(pid, eloBefore);
            tierBeforeMap.put(pid, tierBefore);
            int newElo = Math.max(MIN_ELO, eloBefore + eloChange);
            rating.setElo(newElo);
            rating.setTier(computeTier(newElo));

            // Win/loss/streak tracking
            if (player.getRank() == 1) {
                rating.setWins(rating.getWins() + 1);
                rating.setWinStreak(rating.getWinStreak() + 1);
                // Update best_win_streak if current streak exceeds it
                if (rating.getWinStreak() > rating.getBestWinStreak()) {
                    rating.setBestWinStreak(rating.getWinStreak());
                }
            } else if (player.getRank() == playerCount) {
                rating.setLosses(rating.getLosses() + 1);
                rating.setWinStreak(0);
            } else {
                // Middle placement — draw, streak unchanged
                rating.setDraws(rating.getDraws() + 1);
            }

            playerRatingRepository.save(rating);

            newEloMap.put(pid, newElo);
            newTierMap.put(pid, rating.getTier().name());
        }

        participantRepository.saveAll(players);
    }

    /**
     * Tier thresholds:
     *   0-1199 BRONZE, 1200-1499 SILVER, 1500-1799 GOLD, 1800-2099 DIAMOND, 2100+ LEGEND
     */
    private PlayerTier computeTier(int elo) {
        if (elo >= 2100) return PlayerTier.LEGEND;
        if (elo >= 1800) return PlayerTier.DIAMOND;
        if (elo >= 1500) return PlayerTier.GOLD;
        if (elo >= 1200) return PlayerTier.SILVER;
        return PlayerTier.BRONZE;
    }

    // ──────────────────────────────────────────────
    // 4.2 — updateDailyEntries
    // ──────────────────────────────────────────────

    private void updateDailyEntries(String roomId, BattleRoom room, List<BattleParticipant> players) {
        Optional<DailyChallenge> dailyOpt = dailyChallengeRepository.findByChallengeDate(LocalDate.now());
        if (dailyOpt.isEmpty()) {
            log.warn("No DailyChallenge found for today — skipping daily entry update for room {}", roomId);
            return;
        }

        String dailyChallengeId = dailyOpt.get().getId().toString();

        for (BattleParticipant player : players) {
            Optional<DailyEntry> entryOpt = dailyEntryRepository
                    .findByUserIdAndDailyChallengeId(player.getUserId(), dailyChallengeId);

            DailyEntry entry = entryOpt.orElseGet(() -> DailyEntry.builder()
                    .userId(player.getUserId())
                    .dailyChallengeId(dailyChallengeId)
                    .build());

            entry.setScore(player.getScore());

            // Time = seconds from startsAt to last ACCEPTED submission (or endsAt if no solve)
            String pid = player.getId().toString();
            List<BattleSubmission> subs = submissionRepository.findByParticipantIdOrderBySubmittedAtAsc(pid);
            Optional<BattleSubmission> lastAccepted = subs.stream()
                    .filter(s -> s.getStatus() == BattleSubmissionStatus.ACCEPTED)
                    .reduce((first, second) -> second); // get last

            boolean hasAccepted = lastAccepted.isPresent();

            LocalDateTime endTime = hasAccepted ? lastAccepted.get().getSubmittedAt() : room.getEndsAt();
            if (endTime != null && room.getStartsAt() != null) {
                entry.setTimeSeconds((int) Duration.between(room.getStartsAt(), endTime).getSeconds());
            }

            entry.setStatus(hasAccepted ? DailyEntryStatus.COMPLETED : DailyEntryStatus.FAILED);
            entry.setSubmittedAt(LocalDateTime.now());
            dailyEntryRepository.save(entry);
        }
    }

    // ──────────────────────────────────────────────
    // Response builders
    // ──────────────────────────────────────────────

    /**
     * Builds PostMatchSummaryResponse from already-scored participants (idempotent path).
     */
    public PostMatchSummaryResponse buildPostMatchSummary(BattleRoom room, List<BattleParticipant> players) {
        List<BattleRoomChallenge> roomChallenges = roomChallengeRepository
                .findByRoomIdOrderByPositionAsc(room.getId().toString());
        int matchDurationSeconds = getMatchDurationSeconds(room.getMode());

        // Rebuild breakdowns for display
        Map<String, List<ScoreBreakdownResponse>> breakdownMap = new HashMap<>();
        for (BattleParticipant player : players) {
            String pid = player.getId().toString();
            List<BattleSubmission> subs = submissionRepository.findByParticipantIdOrderBySubmittedAtAsc(pid);

            List<ScoreBreakdownResponse> breakdowns = roomChallenges.stream()
                    .map(rc -> computeChallengeScore(rc, subs, room.getStartsAt(), matchDurationSeconds))
                    .toList();
            breakdownMap.put(pid, breakdowns);
        }

        // Load badges awarded for each participant
        Map<String, List<String>> badgesMap = new HashMap<>();
        for (BattleParticipant player : players) {
            // We'd need to query PlayerBadge by participantId, but for now just return empty
            badgesMap.put(player.getId().toString(), List.of());
        }

        // Reconstruct current ELO/tier snapshot for idempotent path (after-state only).
        Map<String, Integer> newEloMap = new HashMap<>();
        Map<String, String> newTierMap = new HashMap<>();
        if (isRankedMode(room.getMode())) {
            seasonRepository.findFirstByIsActiveTrue().ifPresent(season -> {
                String seasonId = season.getId().toString();
                for (BattleParticipant p : players) {
                    playerRatingRepository.findByUserIdAndSeasonId(p.getUserId(), seasonId).ifPresent(r -> {
                        newEloMap.put(p.getId().toString(), r.getElo());
                        if (r.getTier() != null) newTierMap.put(p.getId().toString(), r.getTier().name());
                    });
                }
            });
        }

        return buildPostMatchSummaryWithDetails(room, players, breakdownMap,
                newEloMap, newTierMap, new HashMap<>(), new HashMap<>(), badgesMap);
    }

    private PostMatchSummaryResponse buildPostMatchSummaryWithDetails(
            BattleRoom room, List<BattleParticipant> players,
            Map<String, List<ScoreBreakdownResponse>> breakdownMap,
            Map<String, Integer> newEloMap, Map<String, String> newTierMap,
            Map<String, Integer> eloBeforeMap, Map<String, String> tierBeforeMap,
            Map<String, List<String>> badgesMap) {

        String roomId = room.getId().toString();

        // Determine finish reason
        List<BattleRoomChallenge> roomChallenges = roomChallengeRepository
                .findByRoomIdOrderByPositionAsc(roomId);
        boolean allFinished = players.stream().allMatch(p -> {
            String pid = p.getId().toString();
            List<BattleSubmission> subs = submissionRepository.findByParticipantIdOrderBySubmittedAtAsc(pid);
            long solved = subs.stream()
                    .filter(s -> s.getStatus() == BattleSubmissionStatus.ACCEPTED)
                    .map(BattleSubmission::getRoomChallengeId).distinct().count();
            return solved >= room.getChallengeCount();
        });
        String finishReason = allFinished ? "ALL_SUBMITTED" : "TIME_LIMIT_REACHED";

        long durationSeconds = 0;
        if (room.getStartsAt() != null && room.getEndsAt() != null) {
            durationSeconds = Duration.between(room.getStartsAt(), room.getEndsAt()).getSeconds();
        }

        // Sort players by rank for standings
        List<BattleParticipant> sorted = players.stream()
                .sorted(Comparator.comparingInt(p -> p.getRank() != null ? p.getRank() : Integer.MAX_VALUE))
                .toList();

        List<PlayerScoreResponse> standings = sorted.stream().map(player -> {
            String pid = player.getId().toString();
            String username = resolveUsername(player.getUserId());
            String avatarUrl = userRepository.findByAuth0Id(player.getUserId())
                    .map(User::getAvatarUrl).orElse(null);

            return PlayerScoreResponse.builder()
                    .participantId(pid)
                    .userId(player.getUserId())
                    .username(username)
                    .avatarUrl(avatarUrl)
                    .finalRank(player.getRank() != null ? player.getRank() : 0)
                    .finalScore(player.getScore() != null ? player.getScore() : 0)
                    .eloChange(player.getEloChange() != null ? player.getEloChange() : 0)
                    .newElo(newEloMap.getOrDefault(pid, 0))
                    .newTier(newTierMap.getOrDefault(pid, null))
                    .eloBefore(eloBeforeMap.get(pid))
                    .eloAfter(newEloMap.get(pid))
                    .tierBefore(tierBeforeMap.get(pid))
                    .tierAfter(newTierMap.get(pid))
                    .tierChanged(tierBeforeMap.get(pid) != null && newTierMap.get(pid) != null
                            && !tierBeforeMap.get(pid).equals(newTierMap.get(pid)))
                    .challengeBreakdowns(breakdownMap.getOrDefault(pid, List.of()))
                    .badgesAwarded(badgesMap.getOrDefault(pid, List.of()))
                    .isWinner(player.getRank() != null && player.getRank() == 1)
                    .build();
        }).toList();

        return PostMatchSummaryResponse.builder()
                .roomId(roomId)
                .mode(room.getMode().name())
                .challengeCount(room.getChallengeCount())
                .startedAt(room.getStartsAt())
                .finishedAt(room.getEndsAt())
                .durationSeconds(durationSeconds)
                .finishReason(finishReason)
                .standings(standings)
                .maxPossibleScore(950 * room.getChallengeCount())
                .build();
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private int getMatchDurationSeconds(BattleMode mode) {
        int minutes = switch (mode) {
            case DUEL -> timeLimitProperties.getDuelMinutes();
            case TEAM -> timeLimitProperties.getTeamMinutes();
            case RANKED_ARENA -> timeLimitProperties.getRankedArenaMinutes();
            case BLITZ -> timeLimitProperties.getBlitzMinutes();
            case PRACTICE -> timeLimitProperties.getPracticeMinutes();
            case DAILY -> timeLimitProperties.getDailyMinutes();
        };
        return minutes > 0 ? minutes * 60 : -1;
    }

    private boolean isRankedMode(BattleMode mode) {
        return mode == BattleMode.DUEL || mode == BattleMode.TEAM || mode == BattleMode.RANKED_ARENA;
    }

    private String resolveUsername(String userId) {
        return userRepository.findByAuth0Id(userId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getFirstName())
                .orElse(userId);
    }
}

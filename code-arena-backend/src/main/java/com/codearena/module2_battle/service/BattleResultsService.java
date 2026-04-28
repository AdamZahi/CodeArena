package com.codearena.module2_battle.service;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module2_battle.dto.PostMatchSummaryResponse;
import com.codearena.module2_battle.dto.ReplayResponse;
import com.codearena.module2_battle.dto.SeasonLeaderboardResponse;
import com.codearena.module2_battle.dto.SeasonLeaderboardEntryResponse;
import com.codearena.module2_battle.dto.ReplaySubmissionResponse;
import com.codearena.module2_battle.dto.ArenaChallengeResponse;
import com.codearena.module2_battle.entity.*;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.BattleSubmissionStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.exception.ActiveSeasonNotFoundException;
import com.codearena.module2_battle.exception.BattleRoomNotFoundException;
import com.codearena.module2_battle.exception.ParticipantNotFoundException;
import com.codearena.module2_battle.exception.ResultsNotReadyException;
import com.codearena.module2_battle.repository.*;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import com.codearena.module2_battle.util.UserDisplayUtils;

/**
 * Read-only service for post-match queries (scoreboard, replay, leaderboard).
 * Does not write anything — all writes happen in BattleScoringService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleResultsService {

    private final BattleRoomRepository battleRoomRepository;
    private final BattleParticipantRepository participantRepository;
    private final BattleRoomChallengeRepository roomChallengeRepository;
    private final BattleSubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final SeasonRepository seasonRepository;
    private final UserRepository userRepository;
    private final BattleScoringService scoringService;

    /**
     * Returns the full post-match scoreboard for a finished room.
     */
    @Transactional(readOnly = true)
    public PostMatchSummaryResponse getPostMatchSummary(String roomId) {
        BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId))
                .orElseThrow(() -> new BattleRoomNotFoundException(roomId));

        if (room.getStatus() != BattleRoomStatus.FINISHED) {
            throw new ResultsNotReadyException(roomId, room.getStatus());
        }

        List<BattleParticipant> players = participantRepository
                .findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);

        return scoringService.buildPostMatchSummary(room, players);
    }

    /**
     * Returns the full chronological replay for a finished room.
     * All submissions from all players, sorted by submittedAt ASC.
     */
    @Transactional(readOnly = true)
    public ReplayResponse getReplay(String roomId, String requestingUserId) {
        BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId))
                .orElseThrow(() -> new BattleRoomNotFoundException(roomId));

        if (room.getStatus() != BattleRoomStatus.FINISHED) {
            throw new ResultsNotReadyException(roomId, room.getStatus());
        }

        // Confirm requesting user is a participant
        participantRepository.findByRoomIdAndUserId(roomId, requestingUserId)
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));

        List<BattleParticipant> players = participantRepository
                .findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);
        List<BattleRoomChallenge> roomChallenges = roomChallengeRepository
                .findByRoomIdOrderByPositionAsc(roomId);

        // Build roomChallengeId -> position map
        Map<String, Integer> positionMap = roomChallenges.stream()
                .collect(Collectors.toMap(rc -> rc.getId().toString(), BattleRoomChallenge::getPosition));

        // Build participantId -> username map
        Map<String, String> usernameMap = players.stream()
                .collect(Collectors.toMap(p -> p.getId().toString(),
                        p -> resolveUsername(p.getUserId())));

        // Collect all submissions from all players
        List<ReplaySubmissionResponse> timeline = new ArrayList<>();
        for (BattleParticipant player : players) {
            String pid = player.getId().toString();
            List<BattleSubmission> subs = submissionRepository.findByParticipantIdOrderBySubmittedAtAsc(pid);

            for (BattleSubmission sub : subs) {
                long secondsFromStart = 0;
                if (room.getStartsAt() != null && sub.getSubmittedAt() != null) {
                    secondsFromStart = Duration.between(room.getStartsAt(), sub.getSubmittedAt()).getSeconds();
                }

                timeline.add(ReplaySubmissionResponse.builder()
                        .submissionId(sub.getId().toString())
                        .participantId(pid)
                        .username(usernameMap.getOrDefault(pid, pid))
                        .challengePosition(positionMap.getOrDefault(sub.getRoomChallengeId(), 0))
                        .language(sub.getLanguage())
                        .status(sub.getStatus().name())
                        .attemptNumber(sub.getAttemptNumber())
                        .runtimeMs(sub.getRuntimeMs())
                        .memoryKb(sub.getMemoryKb())
                        .secondsFromStart(secondsFromStart)
                        .build());
            }
        }

        // Sort by submission time
        timeline.sort(Comparator.comparingLong(ReplaySubmissionResponse::getSecondsFromStart));

        // Build challenge list (reuse arena challenge response from Step 3)
        List<ArenaChallengeResponse> challenges = roomChallenges.stream().map(rc -> {
            Challenge challenge = challengeRepository.findById(Long.parseLong(rc.getChallengeId())).orElse(null);
            ArenaChallengeResponse.ArenaChallengeResponseBuilder builder = ArenaChallengeResponse.builder()
                    .roomChallengeId(rc.getId().toString())
                    .position(rc.getPosition())
                    .challengeId(rc.getChallengeId());
            if (challenge != null) {
                builder.title(challenge.getTitle())
                        .description(challenge.getDescription())
                        .difficulty(challenge.getDifficulty())
                        .tags(challenge.getTags());
            }
            return builder.build();
        }).toList();

        // Get final standings
        PostMatchSummaryResponse summary = scoringService.buildPostMatchSummary(room, players);

        long totalDuration = 0;
        if (room.getStartsAt() != null && room.getEndsAt() != null) {
            totalDuration = Duration.between(room.getStartsAt(), room.getEndsAt()).getSeconds();
        }

        return ReplayResponse.builder()
                .roomId(roomId)
                .challenges(challenges)
                .timeline(timeline)
                .finalStandings(summary.getStandings())
                .totalDurationSeconds(totalDuration)
                .build();
    }

    /**
     * Returns the post-match transparency comparison: every challenge with
     * every player's metrics side-by-side, including the accepted source code
     * so participants can see exactly why the winner won.
     *
     * Only ACCEPTED submission code is exposed — rejected drafts stay private.
     */
    @Transactional(readOnly = true)
    public MatchComparisonResponse getMatchComparison(String roomId, String requestingUserId) {
        BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId))
                .orElseThrow(() -> new BattleRoomNotFoundException(roomId));

        if (room.getStatus() != BattleRoomStatus.FINISHED) {
            throw new ResultsNotReadyException(roomId, room.getStatus());
        }

        // Caller must have been in this room (player or spectator).
        participantRepository.findByRoomIdAndUserId(roomId, requestingUserId)
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));

        List<BattleParticipant> players = participantRepository
                .findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);
        List<BattleRoomChallenge> roomChallenges = roomChallengeRepository
                .findByRoomIdOrderByPositionAsc(roomId);

        // Pre-load the standings (drives final-rank labels and per-challenge breakdowns).
        PostMatchSummaryResponse summary = scoringService.buildPostMatchSummary(room, players);

        // Index per-player breakdowns by roomChallengeId for O(1) lookup.
        // standings entries carry a challengeBreakdowns list in the same order
        // as roomChallenges; we collapse them into (participantId, rcId) -> breakdown.
        Map<String, Map<String, ScoreBreakdownResponse>> breakdownIndex = new HashMap<>();
        for (PlayerScoreResponse standing : summary.getStandings()) {
            Map<String, ScoreBreakdownResponse> perChallenge = new HashMap<>();
            for (ScoreBreakdownResponse bd : standing.getChallengeBreakdowns()) {
                perChallenge.put(bd.getRoomChallengeId(), bd);
            }
            breakdownIndex.put(standing.getParticipantId(), perChallenge);
        }

        // Resolve user-facing fields once per participant.
        Map<String, PlayerScoreResponse> standingByParticipantId = summary.getStandings().stream()
                .collect(Collectors.toMap(PlayerScoreResponse::getParticipantId, s -> s));

        boolean[] anyAi = { false };

        List<ChallengeComparisonResponse> challengeViews = new ArrayList<>();
        for (BattleRoomChallenge rc : roomChallenges) {
            String rcId = rc.getId().toString();

            // Resolve challenge metadata.
            Challenge challenge = null;
            try {
                challenge = challengeRepository.findById(Long.parseLong(rc.getChallengeId())).orElse(null);
            } catch (NumberFormatException ignored) { /* leave as null */ }
            String title = challenge != null ? challenge.getTitle() : "Challenge " + rc.getPosition();
            String difficulty = challenge != null && challenge.getDifficulty() != null
                    ? challenge.getDifficulty() : null;

            // For each player, find their accepted submission for this challenge (if any).
            List<PlayerChallengeAttemptResponse> attempts = new ArrayList<>();
            for (BattleParticipant player : players) {
                String pid = player.getId().toString();
                ScoreBreakdownResponse bd = breakdownIndex
                        .getOrDefault(pid, Map.of()).get(rcId);

                List<BattleSubmission> subs = submissionRepository
                        .findByParticipantIdOrderBySubmittedAtAsc(pid).stream()
                        .filter(s -> rcId.equals(s.getRoomChallengeId()))
                        .toList();
                BattleSubmission accepted = subs.stream()
                        .filter(s -> s.getStatus() == BattleSubmissionStatus.ACCEPTED)
                        .findFirst().orElse(null);

                PlayerScoreResponse standing = standingByParticipantId.get(pid);
                if (accepted != null && accepted.getAiScore() != null) anyAi[0] = true;

                attempts.add(PlayerChallengeAttemptResponse.builder()
                        .participantId(pid)
                        .userId(player.getUserId())
                        .username(standing != null ? standing.getUsername() : resolveUsername(player.getUserId()))
                        .avatarUrl(standing != null ? standing.getAvatarUrl() : null)
                        .finalRank(standing != null ? standing.getFinalRank() : 0)
                        .solved(accepted != null)
                        .attemptCount(subs.size())
                        .solvedInSeconds(bd != null ? bd.getSolvedInSeconds() : -1)
                        .runtimeMs(accepted != null ? accepted.getRuntimeMs() : null)
                        .memoryKb(accepted != null ? accepted.getMemoryKb() : null)
                        .aiScore(accepted != null ? accepted.getAiScore() : null)
                        .aiScoreFallback(accepted != null ? accepted.getAiScoreFallback() : null)
                        .complexityLabel(accepted != null ? accepted.getComplexityLabel() : null)
                        .complexityDisplay(accepted != null ? accepted.getComplexityDisplay() : null)
                        .complexityScore(accepted != null ? accepted.getComplexityScore() : null)
                        .complexityConfidence(accepted != null ? accepted.getComplexityConfidence() : null)
                        .correctnessScore(bd != null ? bd.getCorrectnessScore() : 0)
                        .speedScore(bd != null ? bd.getSpeedScore() : 0)
                        .efficiencyScore(bd != null ? bd.getEfficiencyScore() : 0)
                        .attemptPenalty(bd != null ? bd.getAttemptPenalty() : 0)
                        .totalChallengeScore(bd != null ? bd.getTotalChallengeScore() : 0)
                        .acceptedCode(accepted != null ? accepted.getCode() : null)
                        .language(accepted != null ? accepted.getLanguage() : null)
                        .build());
            }

            // Compute per-challenge highlights so the UI can mark "Fastest", etc.
            decorateHighlights(attempts);

            // Sort: solved first, then by total challenge score DESC, then by solve time ASC.
            attempts.sort(Comparator
                    .comparing(PlayerChallengeAttemptResponse::isSolved).reversed()
                    .thenComparing(PlayerChallengeAttemptResponse::getTotalChallengeScore,
                            Comparator.reverseOrder())
                    .thenComparingLong(a -> a.getSolvedInSeconds() < 0
                            ? Long.MAX_VALUE : a.getSolvedInSeconds()));

            challengeViews.add(ChallengeComparisonResponse.builder()
                    .roomChallengeId(rcId)
                    .position(rc.getPosition())
                    .title(title)
                    .difficulty(difficulty)
                    .attempts(attempts)
                    .build());
        }

        long totalDuration = 0;
        if (room.getStartsAt() != null && room.getEndsAt() != null) {
            totalDuration = Duration.between(room.getStartsAt(), room.getEndsAt()).getSeconds();
        }

        return MatchComparisonResponse.builder()
                .roomId(roomId)
                .mode(room.getMode().name())
                .durationSeconds(totalDuration)
                .standings(summary.getStandings())
                .challenges(challengeViews)
                .scoringFormulaLines(scoringFormulaExplanation())
                .aiScoringAvailable(anyAi[0])
                .build();
    }

    /**
     * Mark the fastest solver, the most-optimized solver (highest aiScore),
     * and the first solver among the players who solved this challenge.
     */
    private static void decorateHighlights(List<PlayerChallengeAttemptResponse> attempts) {
        List<PlayerChallengeAttemptResponse> solvers = attempts.stream()
                .filter(PlayerChallengeAttemptResponse::isSolved)
                .toList();
        if (solvers.isEmpty()) return;

        solvers.stream()
                .filter(a -> a.getRuntimeMs() != null)
                .min(Comparator.comparingInt(PlayerChallengeAttemptResponse::getRuntimeMs))
                .ifPresent(a -> a.setFastest(true));

        solvers.stream()
                .filter(a -> a.getAiScore() != null)
                .max(Comparator.comparingDouble(PlayerChallengeAttemptResponse::getAiScore))
                .ifPresent(a -> a.setMostOptimized(true));

        solvers.stream()
                .filter(a -> a.getSolvedInSeconds() >= 0)
                .min(Comparator.comparingLong(PlayerChallengeAttemptResponse::getSolvedInSeconds))
                .ifPresent(a -> a.setFirstSolver(true));
    }

    private static List<String> scoringFormulaExplanation() {
        return List.of(
                "Total = Σ challenge scores. Per-challenge max is 950.",
                "Correctness: 500 pts for solving (0 otherwise).",
                "Speed: up to 300 pts, linear decay vs match time. Solve early, score higher.",
                "Efficiency: up to 150 pts vs the global runtime/memory baseline for this challenge.",
                "Attempt penalty: −10 per failed attempt, capped at −50.",
                "AI Score (0–100) is an extra optimization rating from the Score Ranker model.",
                "Tiebreak order: total score → total accepted-time → total attempts."
        );
    }

    /**
     * Returns the top 50 players in the active season plus the requesting user's rank.
     */
    @Transactional(readOnly = true)
    public SeasonLeaderboardResponse getSeasonLeaderboard(String requestingUserId) {
        Season activeSeason = seasonRepository.findFirstByIsActiveTrue()
                .orElseThrow(ActiveSeasonNotFoundException::new);

        String seasonId = activeSeason.getId().toString();
        List<PlayerRating> allRatings = playerRatingRepository.findBySeasonIdOrderByEloDesc(seasonId);

        // Build top 50 entries
        List<SeasonLeaderboardEntryResponse> entries = new ArrayList<>();
        Integer requestingUserRank = null;

        for (int i = 0; i < allRatings.size(); i++) {
            PlayerRating rating = allRatings.get(i);
            int rank = i + 1;

            if (rating.getUserId().equals(requestingUserId)) {
                requestingUserRank = rank;
            }

            if (rank <= 50) {
                String username = resolveUsername(rating.getUserId());
                String avatarUrl = userRepository.findByAuth0Id(rating.getUserId())
                        .map(User::getAvatarUrl).orElse(null);

                entries.add(SeasonLeaderboardEntryResponse.builder()
                        .rank(rank)
                        .userId(rating.getUserId())
                        .username(username)
                        .avatarUrl(avatarUrl)
                        .elo(rating.getElo())
                        .tier(rating.getTier().name())
                        .wins(rating.getWins())
                        .losses(rating.getLosses())
                        .winStreak(rating.getWinStreak())
                        .build());
            }
        }

        return SeasonLeaderboardResponse.builder()
                .seasonId(seasonId)
                .seasonName(activeSeason.getName())
                .seasonEndsAt(activeSeason.getEndsAt())
                .entries(entries)
                .requestingUserRank(requestingUserRank)
                .build();
    }

    /**
     * Returns a specific player's season ranking.
     */
    @Transactional(readOnly = true)
    public SeasonLeaderboardEntryResponse getPlayerSeasonRank(String userId) {
        Season activeSeason = seasonRepository.findFirstByIsActiveTrue()
                .orElseThrow(ActiveSeasonNotFoundException::new);

        String seasonId = activeSeason.getId().toString();
        List<PlayerRating> allRatings = playerRatingRepository.findBySeasonIdOrderByEloDesc(seasonId);

        for (int i = 0; i < allRatings.size(); i++) {
            PlayerRating rating = allRatings.get(i);
            if (rating.getUserId().equals(userId)) {
                String username = resolveUsername(userId);
                String avatarUrl = userRepository.findByAuth0Id(userId)
                        .map(User::getAvatarUrl).orElse(null);

                return SeasonLeaderboardEntryResponse.builder()
                        .rank(i + 1)
                        .userId(userId)
                        .username(username)
                        .avatarUrl(avatarUrl)
                        .elo(rating.getElo())
                        .tier(rating.getTier().name())
                        .wins(rating.getWins())
                        .losses(rating.getLosses())
                        .winStreak(rating.getWinStreak())
                        .build();
            }
        }

        // Player has no rating yet — return unranked entry
        String username = resolveUsername(userId);
        String avatarUrl = userRepository.findByKeycloakId(userId)
                .map(User::getAvatarUrl).orElse(null);

        return SeasonLeaderboardEntryResponse.builder()
                .rank(0)
                .userId(userId)
                .username(username)
                .avatarUrl(avatarUrl)
                .elo(1000)
                .tier("BRONZE")
                .wins(0).losses(0).winStreak(0)
                .build();
    }

    private String resolveUsername(String userId) {
        return UserDisplayUtils.resolveDisplayName(userId, userRepository);
    }
}

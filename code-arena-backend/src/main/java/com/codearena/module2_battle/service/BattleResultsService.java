package com.codearena.module2_battle.service;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.entity.*;
import com.codearena.module2_battle.enums.BattleRoomStatus;
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
                String avatarUrl = userRepository.findByKeycloakId(rating.getUserId())
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
                String avatarUrl = userRepository.findByKeycloakId(userId)
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
        return userRepository.findByKeycloakId(userId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getFirstName())
                .orElse(userId);
    }
}

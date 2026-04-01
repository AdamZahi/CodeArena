package com.codearena.module2_battle.service;

import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.entity.*;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.exception.UserNotFoundException;
import com.codearena.module2_battle.repository.*;
import com.codearena.module3_reward.entity.Badge;
import com.codearena.module3_reward.repository.BadgeRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BattleProfileService {

    private final UserRepository userRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final BattleParticipantRepository participantRepository;
    private final BattleSubmissionRepository submissionRepository;
    private final PlayerBadgeRepository playerBadgeRepository;
    private final BadgeRepository badgeRepository;
    private final BattleRoomRepository battleRoomRepository;
    private final SeasonRepository seasonRepository;
    private final DailyStreakCalculator dailyStreakCalculator;

    public BattleProfileResponse getProfile(String targetUserId, String requestingUserId) {
        User user = userRepository.findByKeycloakId(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        String username = user.getNickname() != null ? user.getNickname() : user.getFirstName();

        // Load current season rating (null-safe)
        PlayerRating currentRating = seasonRepository.findByIsActiveTrue()
                .flatMap(season -> playerRatingRepository.findByUserIdAndSeasonId(
                        targetUserId, season.getId().toString()))
                .orElse(null);

        // All-time stats via aggregate queries
        long totalMatchesPlayed = participantRepository.countFinishedByUserId(targetUserId);
        long totalRankedMatchesPlayed = participantRepository.countFinishedRankedByUserId(targetUserId);
        long totalWins = participantRepository.countWinsByUserId(targetUserId);
        double averageScore = participantRepository.averageScoreByUserId(targetUserId);
        long totalBattlePoints = participantRepository.sumScoreByUserId(targetUserId);

        // Total solved challenges: count ACCEPTED submissions across all participant records
        List<BattleParticipant> allParticipants = participantRepository.findByUserId(targetUserId);
        List<String> participantIds = allParticipants.stream()
                .map(p -> p.getId().toString()).toList();
        long totalSolvedChallenges = participantIds.isEmpty() ? 0
                : submissionRepository.countAcceptedByParticipantIds(participantIds);

        // Best win streak across all seasons
        int bestWinStreak = playerRatingRepository.findBestWinStreakByUserId(targetUserId);

        // Badges
        List<PlayerBadge> playerBadges = playerBadgeRepository.findByUserIdOrderByAwardedAtDesc(targetUserId);
        List<EarnedBadgeResponse> badges = playerBadges.stream().map(pb -> {
            Badge badge = badgeRepository.findById(UUID.fromString(pb.getBadgeId())).orElse(null);
            // Resolve roomId from participantId if present
            String roomId = null;
            if (pb.getParticipantId() != null) {
                roomId = participantRepository.findById(UUID.fromString(pb.getParticipantId()))
                        .map(BattleParticipant::getRoomId).orElse(null);
            }
            return EarnedBadgeResponse.builder()
                    .badgeId(pb.getBadgeId())
                    .key(badge != null ? badge.getCriteria() : null)
                    .name(badge != null ? badge.getName() : null)
                    .description(badge != null ? badge.getDescription() : null)
                    .iconUrl(badge != null ? badge.getIconUrl() : null)
                    .awardedAt(pb.getAwardedAt())
                    .roomId(roomId)
                    .build();
        }).toList();

        // Recent 5 matches
        List<BattleParticipant> recentParticipants = participantRepository
                .findFinishedByUserIdOrderByJoinedAtDesc(targetUserId, PageRequest.of(0, 5));
        List<MatchHistorySummaryResponse> recentMatches = recentParticipants.stream()
                .map(this::buildMatchSummary)
                .filter(Objects::nonNull)
                .toList();

        // Daily streaks
        int dailyStreak = dailyStreakCalculator.computeCurrentStreak(targetUserId);
        int longestDailyStreak = dailyStreakCalculator.computeLongestStreak(targetUserId);

        // Round averageScore to 1 decimal place
        averageScore = Math.round(averageScore * 10.0) / 10.0;

        return BattleProfileResponse.builder()
                .userId(targetUserId)
                .username(username)
                .avatarUrl(user.getAvatarUrl())
                .currentElo(currentRating != null ? currentRating.getElo() : null)
                .currentTier(currentRating != null ? currentRating.getTier().name() : null)
                .seasonWins(currentRating != null ? currentRating.getWins() : 0)
                .seasonLosses(currentRating != null ? currentRating.getLosses() : 0)
                .seasonDraws(currentRating != null ? currentRating.getDraws() : 0)
                .currentWinStreak(currentRating != null ? currentRating.getWinStreak() : 0)
                .bestWinStreak(bestWinStreak)
                .totalMatchesPlayed((int) totalMatchesPlayed)
                .totalRankedMatchesPlayed((int) totalRankedMatchesPlayed)
                .totalWins((int) totalWins)
                .totalSolvedChallenges((int) totalSolvedChallenges)
                .averageScore(averageScore)
                .totalBattlePoints((int) totalBattlePoints)
                .badges(badges)
                .recentMatches(recentMatches)
                .dailyStreak(dailyStreak)
                .longestDailyStreak(longestDailyStreak)
                .build();
    }

    /**
     * Builds a MatchHistorySummaryResponse from a BattleParticipant record.
     */
    MatchHistorySummaryResponse buildMatchSummary(BattleParticipant participant) {
        Optional<BattleRoom> roomOpt = battleRoomRepository.findById(
                UUID.fromString(participant.getRoomId()));
        if (roomOpt.isEmpty()) return null;
        BattleRoom room = roomOpt.get();

        long durationSeconds = 0;
        if (room.getStartsAt() != null && room.getEndsAt() != null) {
            durationSeconds = Duration.between(room.getStartsAt(), room.getEndsAt()).getSeconds();
        }

        int totalPlayers = participantRepository.countByRoomIdAndRole(
                participant.getRoomId(), ParticipantRole.PLAYER);

        // Build opponent summary
        String opponentSummary = buildOpponentSummary(room, participant, totalPlayers);

        // Badges earned in this match
        List<PlayerBadge> matchBadges = playerBadgeRepository.findByParticipantId(
                participant.getId().toString());
        List<String> badgesEarned = matchBadges.stream().map(pb ->
                badgeRepository.findById(UUID.fromString(pb.getBadgeId()))
                        .map(Badge::getName).orElse("Unknown")
        ).toList();

        return MatchHistorySummaryResponse.builder()
                .roomId(participant.getRoomId())
                .mode(room.getMode().name())
                .status(BattleRoomStatus.FINISHED)
                .playedAt(room.getEndsAt())
                .durationSeconds(durationSeconds)
                .finalRank(participant.getRank() != null ? participant.getRank() : 0)
                .finalScore(participant.getScore() != null ? participant.getScore() : 0)
                .totalPlayers(totalPlayers)
                .eloChange(participant.getEloChange() != null ? participant.getEloChange() : 0)
                .isWinner(participant.getRank() != null && participant.getRank() == 1)
                .opponentSummary(opponentSummary)
                .badgesEarned(badgesEarned)
                .build();
    }

    /**
     * Builds opponent summary string:
     * - DUEL/TEAM: "vs. alice, bob" (max 3 names, then "+ N more")
     * - RANKED_ARENA: "8-player tournament"
     */
    private String buildOpponentSummary(BattleRoom room, BattleParticipant self, int totalPlayers) {
        switch (room.getMode()) {
            case DUEL, TEAM -> {
                List<BattleParticipant> others = participantRepository
                        .findByRoomIdAndRole(self.getRoomId(), ParticipantRole.PLAYER)
                        .stream()
                        .filter(p -> !p.getUserId().equals(self.getUserId()))
                        .toList();

                List<String> names = others.stream()
                        .limit(3)
                        .map(p -> resolveUsername(p.getUserId()))
                        .toList();

                String summary = "vs. " + String.join(", ", names);
                if (others.size() > 3) {
                    summary += " + " + (others.size() - 3) + " more";
                }
                return summary;
            }
            default -> {
                return totalPlayers + "-player tournament";
            }
        }
    }

    private String resolveUsername(String userId) {
        return userRepository.findByKeycloakId(userId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getFirstName())
                .orElse(userId);
    }
}

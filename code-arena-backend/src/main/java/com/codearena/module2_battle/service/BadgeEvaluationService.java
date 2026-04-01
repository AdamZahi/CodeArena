package com.codearena.module2_battle.service;

import com.codearena.module2_battle.entity.*;
import com.codearena.module2_battle.enums.BattleMode;
import com.codearena.module2_battle.enums.BattleSubmissionStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.repository.*;
import com.codearena.module3_reward.entity.Badge;
import com.codearena.module3_reward.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Evaluates each player against every badge criterion after a match finishes
 * and awards badges they have not already received.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeEvaluationService {

    private final BadgeRepository badgeRepository;
    private final PlayerBadgeRepository playerBadgeRepository;
    private final BattleSubmissionRepository submissionRepository;
    private final BattleRoomChallengeRepository roomChallengeRepository;
    private final BattleParticipantRepository participantRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final SeasonRepository seasonRepository;

    /**
     * Evaluates all 5 badge criteria for every player and awards badges for newly earned ones.
     * Returns a map of participantId -> list of awarded badge names.
     */
    public Map<String, List<String>> evaluateAndAward(String roomId, BattleRoom room,
                                                       List<BattleParticipant> players) {
        Map<String, List<String>> awarded = new HashMap<>();

        List<BattleRoomChallenge> roomChallenges = roomChallengeRepository.findByRoomIdOrderByPositionAsc(roomId);

        for (BattleParticipant player : players) {
            List<String> badges = new ArrayList<>();
            String participantId = player.getId().toString();
            String userId = player.getUserId();

            List<BattleSubmission> submissions = submissionRepository
                    .findByParticipantIdOrderBySubmittedAtAsc(participantId);

            if (checkSpeedDemon(submissions, room)) {
                awardBadge("speed_demon", userId, participantId, badges);
            }
            if (checkFlawless(submissions)) {
                awardBadge("flawless", userId, participantId, badges);
            }
            if (checkComebackKing(player, players, roomChallenges, room)) {
                awardBadge("comeback_king", userId, participantId, badges);
            }
            if (checkUntouchable(userId, room.getMode())) {
                awardBadge("untouchable", userId, participantId, badges);
            }
            if (checkTournamentChampion(player, room.getMode())) {
                awardBadge("tournament_champion", userId, participantId, badges);
            }

            awarded.put(participantId, badges);
        }

        return awarded;
    }

    private void awardBadge(String criteriaSlug, String userId, String participantId, List<String> badges) {
        Optional<Badge> badgeOpt = badgeRepository.findByCriteria(criteriaSlug);
        if (badgeOpt.isEmpty()) {
            log.warn("Badge with criteria '{}' not found in badge table", criteriaSlug);
            return;
        }

        Badge badge = badgeOpt.get();
        String badgeId = badge.getId().toString();

        // Do not award the same badge to the same user twice
        if (playerBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)) {
            return;
        }

        PlayerBadge playerBadge = PlayerBadge.builder()
                .userId(userId)
                .badgeId(badgeId)
                .participantId(participantId)
                .awardedAt(LocalDateTime.now())
                .build();
        playerBadgeRepository.save(playerBadge);
        badges.add(badge.getName());
        log.info("Awarded badge '{}' to user {}", badge.getName(), userId);
    }

    /**
     * speed_demon: Player has at least one ACCEPTED submission where
     * submittedAt - room.startsAt < 60 seconds.
     */
    private boolean checkSpeedDemon(List<BattleSubmission> submissions, BattleRoom room) {
        if (room.getStartsAt() == null) return false;

        return submissions.stream()
                .filter(s -> s.getStatus() == BattleSubmissionStatus.ACCEPTED)
                .anyMatch(s -> {
                    long seconds = Duration.between(room.getStartsAt(), s.getSubmittedAt()).getSeconds();
                    return seconds < 60;
                });
    }

    /**
     * flawless: Every submission the player made was ACCEPTED (zero failures),
     * AND they solved at least one challenge.
     */
    private boolean checkFlawless(List<BattleSubmission> submissions) {
        if (submissions.isEmpty()) return false;

        boolean hasAccepted = submissions.stream()
                .anyMatch(s -> s.getStatus() == BattleSubmissionStatus.ACCEPTED);
        if (!hasAccepted) return false;

        return submissions.stream()
                .noneMatch(s -> s.getStatus() == BattleSubmissionStatus.WRONG_ANSWER
                        || s.getStatus() == BattleSubmissionStatus.TIME_LIMIT
                        || s.getStatus() == BattleSubmissionStatus.RUNTIME_ERROR
                        || s.getStatus() == BattleSubmissionStatus.COMPILE_ERROR);
    }

    /**
     * comeback_king: After challenge 1, player was in last place (or tied last),
     * but their final_rank == 1 (they won overall).
     *
     * Computes an intermediate rank based only on challenge-1 submissions:
     * - For each player, compute challenge-1 score (just correctness + attempt count as tiebreaker)
     * - Player must be last in that intermediate ranking AND first in final ranking
     */
    private boolean checkComebackKing(BattleParticipant player, List<BattleParticipant> allPlayers,
                                      List<BattleRoomChallenge> roomChallenges, BattleRoom room) {
        if (player.getRank() == null || player.getRank() != 1) return false;
        if (allPlayers.size() < 2) return false;

        // Find position-1 challenge
        Optional<BattleRoomChallenge> challenge1Opt = roomChallenges.stream()
                .filter(rc -> rc.getPosition() == 1)
                .findFirst();
        if (challenge1Opt.isEmpty()) return false;

        String challenge1Id = challenge1Opt.get().getId().toString();

        // Compute intermediate rankings based on challenge 1 only
        // Score: 1 if solved, 0 if not. Tiebreaker: fewer attempts.
        record IntermediateRank(String participantId, boolean solved, int attempts) {}

        List<IntermediateRank> intermediateRanks = allPlayers.stream().map(p -> {
            String pId = p.getId().toString();
            List<BattleSubmission> subs = submissionRepository.findByParticipantIdOrderBySubmittedAtAsc(pId);
            List<BattleSubmission> challenge1Subs = subs.stream()
                    .filter(s -> s.getRoomChallengeId().equals(challenge1Id))
                    .toList();

            boolean solved = challenge1Subs.stream()
                    .anyMatch(s -> s.getStatus() == BattleSubmissionStatus.ACCEPTED);
            int attempts = challenge1Subs.size();

            return new IntermediateRank(pId, solved, attempts);
        }).sorted(Comparator
                .comparing(IntermediateRank::solved).reversed()  // solved first
                .thenComparingInt(IntermediateRank::attempts))   // fewer attempts first
                .toList();

        // Check if this player was in last place (or tied for last)
        String playerParticipantId = player.getId().toString();
        IntermediateRank lastPlace = intermediateRanks.get(intermediateRanks.size() - 1);
        IntermediateRank playerRank = intermediateRanks.stream()
                .filter(ir -> ir.participantId().equals(playerParticipantId))
                .findFirst().orElse(null);

        if (playerRank == null) return false;

        // Player is in last place if they have the same solved/attempts as the actual last place
        return playerRank.solved() == lastPlace.solved() && playerRank.attempts() == lastPlace.attempts();
    }

    /**
     * untouchable: Player's current win_streak >= 5 AND this match is ranked.
     */
    private boolean checkUntouchable(String userId, BattleMode mode) {
        if (!isRankedMode(mode)) return false;

        Optional<Season> activeSeason = seasonRepository.findFirstByIsActiveTrue();
        if (activeSeason.isEmpty()) return false;

        Optional<PlayerRating> rating = playerRatingRepository
                .findByUserIdAndSeasonId(userId, activeSeason.get().getId().toString());

        return rating.isPresent() && rating.get().getWinStreak() >= 5;
    }

    /**
     * tournament_champion: Room mode == RANKED_ARENA AND player's final_rank == 1.
     */
    private boolean checkTournamentChampion(BattleParticipant player, BattleMode mode) {
        return mode == BattleMode.RANKED_ARENA && player.getRank() != null && player.getRank() == 1;
    }

    private boolean isRankedMode(BattleMode mode) {
        return mode == BattleMode.DUEL || mode == BattleMode.TEAM || mode == BattleMode.RANKED_ARENA;
    }
}

package com.codearena.module2_battle.service;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module2_battle.dto.LeaderboardPageResponse;
import com.codearena.module2_battle.dto.LeaderboardPageRequest;
import com.codearena.module2_battle.dto.XpLeaderboardPageResponse;
import com.codearena.module2_battle.dto.DailyLeaderboardResponse;
import com.codearena.module2_battle.dto.SeasonLeaderboardEntryResponse;
import com.codearena.module2_battle.dto.XpLeaderboardEntryResponse;
import com.codearena.module2_battle.dto.DailyLeaderboardEntryResponse;
import com.codearena.module2_battle.entity.*;
import com.codearena.module2_battle.enums.DailyEntryStatus;
import com.codearena.module2_battle.enums.PlayerTier;
import com.codearena.module2_battle.exception.ActiveSeasonNotFoundException;
import com.codearena.module2_battle.exception.DailyChallengeNotFoundException;
import com.codearena.module2_battle.repository.*;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.codearena.module2_battle.util.UserDisplayUtils;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final SeasonRepository seasonRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final UserRepository userRepository;
    private final DailyChallengeRepository dailyChallengeRepository;
    private final DailyEntryRepository dailyEntryRepository;
    private final ChallengeRepository challengeRepository;
    private final DailyStreakCalculator dailyStreakCalculator;

    public LeaderboardPageResponse getSeasonLeaderboardPage(
            String requestingUserId, LeaderboardPageRequest request) {

        Season activeSeason = seasonRepository.findFirstByIsActiveTrue()
                .orElseThrow(ActiveSeasonNotFoundException::new);
        String seasonId = activeSeason.getId().toString();

        // Clamp size to max 100
        int size = Math.min(Math.max(request.getSize(), 1), 100);
        int page = Math.max(request.getPage(), 0);

        // Query with optional tier filter
        Page<PlayerRating> ratingsPage;
        if (request.getTier() != null && !request.getTier().isBlank()) {
            PlayerTier tier = PlayerTier.valueOf(request.getTier().toUpperCase());
            ratingsPage = playerRatingRepository.findBySeasonIdAndTierOrderByEloDescWinsDesc(
                    seasonId, tier, PageRequest.of(page, size));
        } else {
            ratingsPage = playerRatingRepository.findBySeasonIdOrderByEloDescWinsDesc(
                    seasonId, PageRequest.of(page, size));
        }

        // Build entries with rank calculation
        List<SeasonLeaderboardEntryResponse> entries = new ArrayList<>();
        int baseRank = page * size + 1;
        List<PlayerRating> ratings = ratingsPage.getContent();

        // If search filter is provided, filter in-memory (username prefix, case-insensitive)
        if (request.getSearch() != null && !request.getSearch().isBlank()) {
            String prefix = request.getSearch().toLowerCase();
            ratings = ratings.stream().filter(r -> {
                String username = resolveUsername(r.getUserId());
                return username.toLowerCase().startsWith(prefix);
            }).toList();
        }

        for (int i = 0; i < ratings.size(); i++) {
            PlayerRating r = ratings.get(i);
            User user = userRepository.findByAuth0Id(r.getUserId()).orElse(null);
            entries.add(SeasonLeaderboardEntryResponse.builder()
                    .rank(baseRank + i)
                    .userId(r.getUserId())
                    .username(UserDisplayUtils.resolveDisplayName(user))
                    .avatarUrl(UserDisplayUtils.resolveAvatarUrl(user))
                    .elo(r.getElo())
                    .tier(r.getTier().name())
                    .wins(r.getWins())
                    .losses(r.getLosses())
                    .winStreak(r.getWinStreak())
                    .build());
        }

        // Compute requesting user's rank and entry
        Integer requestingUserRank = null;
        SeasonLeaderboardEntryResponse requestingUserEntry = null;
        if (requestingUserId != null) {
            Optional<PlayerRating> userRating = playerRatingRepository
                    .findByUserIdAndSeasonId(requestingUserId, seasonId);
            if (userRating.isPresent()) {
                PlayerRating ur = userRating.get();
                long aboveCount = playerRatingRepository.countPlayersAboveElo(seasonId, ur.getElo());
                requestingUserRank = (int) aboveCount + 1;

                User user = userRepository.findByAuth0Id(requestingUserId).orElse(null);
                requestingUserEntry = SeasonLeaderboardEntryResponse.builder()
                        .rank(requestingUserRank)
                        .userId(ur.getUserId())
                        .username(UserDisplayUtils.resolveDisplayName(user))
                        .avatarUrl(UserDisplayUtils.resolveAvatarUrl(user))
                        .elo(ur.getElo())
                        .tier(ur.getTier().name())
                        .wins(ur.getWins())
                        .losses(ur.getLosses())
                        .winStreak(ur.getWinStreak())
                        .build();
            }
        }

        long daysRemaining = 0;
        if (activeSeason.getEndsAt() != null) {
            long seconds = Duration.between(LocalDateTime.now(), activeSeason.getEndsAt()).getSeconds();
            daysRemaining = (long) Math.ceil(seconds / 86400.0);
            if (daysRemaining < 0) daysRemaining = 0;
        }

        return LeaderboardPageResponse.builder()
                .seasonId(seasonId)
                .seasonName(activeSeason.getName())
                .seasonEndsAt(activeSeason.getEndsAt())
                .daysRemaining(daysRemaining)
                .totalEntries((int) ratingsPage.getTotalElements())
                .page(page)
                .size(size)
                .entries(entries)
                .requestingUserRank(requestingUserRank)
                .requestingUserEntry(requestingUserEntry)
                .build();
    }

    public XpLeaderboardPageResponse getXpLeaderboard(
            String requestingUserId, LeaderboardPageRequest request) {

        int size = Math.min(Math.max(request.getSize(), 1), 100);
        int page = Math.max(request.getPage(), 0);

        Page<User> usersPage = userRepository.findAllByOrderByTotalXpDesc(PageRequest.of(page, size));

        List<XpLeaderboardEntryResponse> entries = new ArrayList<>();
        int baseRank = page * size + 1;
        List<User> users = usersPage.getContent();

        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            entries.add(XpLeaderboardEntryResponse.builder()
                    .rank(baseRank + i)
                    .userId(u.getAuth0Id())
                    .username(UserDisplayUtils.resolveDisplayName(u))
                    .avatarUrl(UserDisplayUtils.resolveAvatarUrl(u))
                    .totalXp(u.getTotalXp())
                    .level(u.getCurrentLevel())
                    .title(u.getActiveTitle())
                    .build());
        }

        Integer requestingUserRank = null;
        XpLeaderboardEntryResponse requestingUserEntry = null;
        if (requestingUserId != null) {
            Optional<User> userOpt = userRepository.findByAuth0Id(requestingUserId);
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                requestingUserRank = userRepository.countUsersByTotalXpGreaterThan(u.getTotalXp());

                requestingUserEntry = XpLeaderboardEntryResponse.builder()
                        .rank(requestingUserRank)
                        .userId(u.getAuth0Id())
                        .username(UserDisplayUtils.resolveDisplayName(u))
                        .avatarUrl(UserDisplayUtils.resolveAvatarUrl(u))
                        .totalXp(u.getTotalXp())
                        .level(u.getCurrentLevel())
                        .title(u.getActiveTitle())
                        .build();
            }
        }

        return XpLeaderboardPageResponse.builder()
                .totalEntries((int) usersPage.getTotalElements())
                .page(page)
                .size(size)
                .entries(entries)
                .requestingUserRank(requestingUserRank)
                .requestingUserEntry(requestingUserEntry)
                .build();
    }

    public DailyLeaderboardResponse getDailyLeaderboard(LocalDate date, String requestingUserId) {
        DailyChallenge dailyChallenge = dailyChallengeRepository.findFirstByChallengeDate(date)
                .orElseThrow(() -> new DailyChallengeNotFoundException(date));

        String dcId = dailyChallenge.getId().toString();

        // Resolve challenge titles
        List<String> challengeTitles = dailyChallenge.getChallengeIds().stream()
                .map(cid -> {
                    try {
                        return challengeRepository.findById(Long.parseLong(cid))
                                .map(Challenge::getTitle).orElse("Unknown");
                    } catch (NumberFormatException e) {
                        return "Unknown";
                    }
                }).toList();

        // Get all COMPLETED entries, ordered by score DESC, timeSeconds ASC
        List<DailyEntry> completedEntries = dailyEntryRepository
                .findByDailyChallengeIdAndStatus(dcId, DailyEntryStatus.COMPLETED);
        completedEntries.sort(Comparator
                .comparingInt((DailyEntry e) -> e.getScore() != null ? e.getScore() : 0).reversed()
                .thenComparingInt(e -> e.getTimeSeconds() != null ? e.getTimeSeconds() : Integer.MAX_VALUE));

        // Get active season for tier lookup
        String activeSeasonId = seasonRepository.findFirstByIsActiveTrue()
                .map(s -> s.getId().toString()).orElse(null);

        Integer requestingUserRank = null;
        List<DailyLeaderboardEntryResponse> entries = new ArrayList<>();

        for (int i = 0; i < completedEntries.size(); i++) {
            DailyEntry entry = completedEntries.get(i);
            int rank = i + 1;

            User user = userRepository.findByAuth0Id(entry.getUserId()).orElse(null);
            String tier = null;
            if (activeSeasonId != null) {
                tier = playerRatingRepository.findByUserIdAndSeasonId(entry.getUserId(), activeSeasonId)
                        .map(pr -> pr.getTier().name()).orElse(null);
            }

            int dailyStreak = dailyStreakCalculator.computeCurrentStreak(entry.getUserId());

            entries.add(DailyLeaderboardEntryResponse.builder()
                    .rank(rank)
                    .userId(entry.getUserId())
                    .username(resolveUsername(entry.getUserId()))
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .score(entry.getScore() != null ? entry.getScore() : 0)
                    .timeSeconds(entry.getTimeSeconds() != null ? entry.getTimeSeconds() : 0)
                    .tier(tier)
                    .dailyStreak(dailyStreak)
                    .build());

            if (requestingUserId != null && entry.getUserId().equals(requestingUserId)) {
                requestingUserRank = rank;
            }
        }

        return DailyLeaderboardResponse.builder()
                .date(date)
                .challengeTitles(challengeTitles)
                .entries(entries)
                .requestingUserRank(requestingUserRank)
                .totalParticipants(completedEntries.size())
                .build();
    }

    private String resolveUsername(String userId) {
        return UserDisplayUtils.resolveDisplayName(userId, userRepository);
    }
}

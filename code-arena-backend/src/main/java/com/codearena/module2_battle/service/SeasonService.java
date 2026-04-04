package com.codearena.module2_battle.service;

import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.entity.PlayerBadge;
import com.codearena.module2_battle.entity.PlayerRating;
import com.codearena.module2_battle.entity.Season;
import com.codearena.module2_battle.enums.PlayerTier;
import com.codearena.module2_battle.exception.ActiveSeasonNotFoundException;
import com.codearena.module2_battle.exception.SeasonAlreadyActiveException;
import com.codearena.module2_battle.repository.PlayerBadgeRepository;
import com.codearena.module2_battle.repository.PlayerRatingRepository;
import com.codearena.module2_battle.repository.SeasonRepository;
import com.codearena.module3_reward.entity.Badge;
import com.codearena.module3_reward.repository.BadgeRepository;
import com.codearena.user.entity.Role;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final PlayerBadgeRepository playerBadgeRepository;
    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;

    @Transactional
    public SeasonSummaryResponse closeActiveSeason(String adminUserId) {
        verifyAdmin(adminUserId);

        Season activeSeason = seasonRepository.findFirstByIsActiveTrue()
                .orElseThrow(ActiveSeasonNotFoundException::new);

        // Compute final standings
        List<PlayerRating> standings = playerRatingRepository
                .findBySeasonIdOrderByEloDesc(activeSeason.getId().toString());

        // Award season_champion badge to top 3
        Badge championBadge = badgeRepository.findFirstByCriteria("season_champion")
                .orElseGet(() -> {
                    Badge b = Badge.builder()
                            .id(UUID.randomUUID())
                            .name("Season Champion")
                            .description("Finished in the top 3 of a ranked season")
                            .criteria("season_champion")
                            .build();
                    return badgeRepository.save(b);
                });

        for (int i = 0; i < Math.min(3, standings.size()); i++) {
            PlayerRating pr = standings.get(i);
            String badgeId = championBadge.getId().toString();
            if (!playerBadgeRepository.existsByUserIdAndBadgeId(pr.getUserId(), badgeId)) {
                PlayerBadge pb = PlayerBadge.builder()
                        .userId(pr.getUserId())
                        .badgeId(badgeId)
                        .awardedAt(LocalDateTime.now())
                        .build();
                playerBadgeRepository.save(pb);
            }
        }

        // Deactivate season
        activeSeason.setIsActive(false);
        if (activeSeason.getEndsAt().isAfter(LocalDateTime.now())) {
            activeSeason.setEndsAt(LocalDateTime.now());
        }
        seasonRepository.save(activeSeason);

        return buildSeasonSummary(activeSeason, standings);
    }

    @Transactional
    public SeasonResponse createNextSeason(String adminUserId, CreateSeasonRequest request) {
        verifyAdmin(adminUserId);

        if (seasonRepository.findFirstByIsActiveTrue().isPresent()) {
            throw new SeasonAlreadyActiveException();
        }

        // Find the most recent (previous) season to carry over ratings
        List<Season> allSeasons = seasonRepository.findAllByOrderByCreatedAtDesc();
        Season previousSeason = allSeasons.isEmpty() ? null : allSeasons.get(0);

        // Create new season
        Season newSeason = Season.builder()
                .name(request.getName())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .isActive(true)
                .build();
        seasonRepository.save(newSeason);

        // Soft reset: carry over ratings from previous season
        // ELO formula: max(1000, previousElo - 200) — players keep ~80% of standing, floored at 1000
        if (previousSeason != null) {
            List<PlayerRating> previousRatings = playerRatingRepository
                    .findBySeasonId(previousSeason.getId().toString());

            for (PlayerRating prev : previousRatings) {
                int newElo = Math.max(1000, prev.getElo() - 200);
                PlayerRating newRating = PlayerRating.builder()
                        .userId(prev.getUserId())
                        .seasonId(newSeason.getId().toString())
                        .elo(newElo)
                        .tier(computeTier(newElo))
                        .wins(0)
                        .losses(0)
                        .draws(0)
                        .winStreak(0)
                        .bestWinStreak(prev.getBestWinStreak())
                        .build();
                playerRatingRepository.save(newRating);
            }
            log.info("Soft-reset {} player ratings from season '{}' to '{}'",
                    previousRatings.size(), previousSeason.getName(), newSeason.getName());
        }

        return toSeasonResponse(newSeason);
    }

    public List<SeasonSummaryResponse> getSeasonHistory() {
        List<Season> seasons = seasonRepository.findAllByOrderByCreatedAtDesc();
        return seasons.stream().map(season -> {
            List<PlayerRating> standings = playerRatingRepository
                    .findBySeasonIdOrderByEloDesc(season.getId().toString());
            return buildSeasonSummary(season, standings);
        }).toList();
    }

    private SeasonSummaryResponse buildSeasonSummary(Season season, List<PlayerRating> standings) {
        List<SeasonTopFinisherResponse> topThree = new ArrayList<>();
        for (int i = 0; i < Math.min(3, standings.size()); i++) {
            PlayerRating pr = standings.get(i);
            User user = userRepository.findByKeycloakId(pr.getUserId()).orElse(null);
            topThree.add(SeasonTopFinisherResponse.builder()
                    .rank(i + 1)
                    .userId(pr.getUserId())
                    .username(user != null ? (user.getNickname() != null ? user.getNickname() : user.getFirstName()) : pr.getUserId())
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .finalElo(pr.getElo())
                    .finalTier(pr.getTier().name())
                    .wins(pr.getWins())
                    .losses(pr.getLosses())
                    .build());
        }

        return SeasonSummaryResponse.builder()
                .season(toSeasonResponse(season))
                .totalParticipants(standings.size())
                .topThree(topThree)
                .build();
    }

    private SeasonResponse toSeasonResponse(Season season) {
        return SeasonResponse.builder()
                .id(season.getId().toString())
                .name(season.getName())
                .startsAt(season.getStartsAt())
                .endsAt(season.getEndsAt())
                .isActive(season.getIsActive())
                .build();
    }

    /**
     * Tier thresholds:
     * 0-1199 BRONZE, 1200-1499 SILVER, 1500-1799 GOLD, 1800-2099 DIAMOND, 2100+ LEGEND
     */
    private PlayerTier computeTier(int elo) {
        if (elo >= 2100) return PlayerTier.LEGEND;
        if (elo >= 1800) return PlayerTier.DIAMOND;
        if (elo >= 1500) return PlayerTier.GOLD;
        if (elo >= 1200) return PlayerTier.SILVER;
        return PlayerTier.BRONZE;
    }

    private void verifyAdmin(String userId) {
        User user = userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied");
        }
    }
}

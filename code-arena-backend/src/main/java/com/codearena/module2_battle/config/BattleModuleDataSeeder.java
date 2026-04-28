package com.codearena.module2_battle.config;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module2_battle.entity.DailyChallenge;
import com.codearena.module2_battle.entity.Season;
import com.codearena.module2_battle.repository.DailyChallengeRepository;
import com.codearena.module2_battle.repository.SeasonRepository;
import com.codearena.module3_reward.entity.Badge;
import com.codearena.module3_reward.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BattleModuleDataSeeder {

    private final SeasonRepository seasonRepository;
    private final BadgeRepository badgeRepository;
    private final ChallengeRepository challengeRepository;
    private final DailyChallengeRepository dailyChallengeRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        try {
            seedSeason();
            seedBattleBadges();
            seedDailyChallenge();
        } catch (DataAccessException ex) {
            log.warn("Skipping battle module seeding because the database is unavailable: {}", ex.getMostSpecificCause().getMessage());
        }
    }

    /**
     * Insert "Season 1" if no active season exists.
     * Single-active-season constraint: if another season is already active, skip and warn.
     */
    private void seedSeason() {
        if (seasonRepository.findFirstByIsActiveTrue().isPresent()) {
            log.info("An active season already exists — skipping Season 1 seed");
            return;
        }

        Season season = Season.builder()
                .name("Season 1")
                .startsAt(LocalDateTime.of(2026, 4, 1, 0, 0, 0))
                .endsAt(LocalDateTime.of(2026, 6, 30, 23, 59, 59))
                .isActive(true)
                .build();
        seasonRepository.save(season);
        log.info("Seeded Season 1 (active)");
    }

    /**
     * Insert battle badge definitions into the existing badge table.
     * Idempotent: skips badges whose criteria slug already exists.
     */
    private void seedBattleBadges() {
        Map<String, String[]> badges = Map.of(
                "speed_demon", new String[]{"Speed Demon", "Solved a challenge in under 60 seconds"},
                "flawless", new String[]{"Flawless", "Completed a battle with zero wrong submissions"},
                "comeback_king", new String[]{"Comeback King", "Won a battle after being in last place after challenge 1"},
                "untouchable", new String[]{"Untouchable", "Won 5 consecutive ranked battles"},
                "tournament_champion", new String[]{"Tournament Champion", "Won a Ranked Arena tournament"}
        );

        List<Badge> existingBadges = badgeRepository.findAll();
        for (var entry : badges.entrySet()) {
            String slug = entry.getKey();
            boolean exists = existingBadges.stream()
                    .anyMatch(b -> slug.equals(b.getCriteria()));
            if (exists) {
                log.debug("Badge '{}' already exists — skipping", slug);
                continue;
            }

            Badge badge = Badge.builder()
                    .id(UUID.randomUUID())
                    .name(entry.getValue()[0])
                    .description(entry.getValue()[1])
                    .criteria(slug)
                    .build();
            badgeRepository.save(badge);
            log.info("Seeded badge: {}", entry.getValue()[0]);
        }
    }

    /**
     * Insert a daily challenge for today using the first 3 challenges found.
     * Skips if today's record already exists or if fewer than 3 challenges are available.
     */
    private void seedDailyChallenge() {
        LocalDate today = LocalDate.now();
        if (dailyChallengeRepository.findByChallengeDate(today).isPresent()) {
            log.info("Daily challenge for {} already exists — skipping", today);
            return;
        }

        List<Challenge> challenges;
        try {
            challenges = challengeRepository.findAll(PageRequest.of(0, 3)).getContent();
        } catch (DataAccessException ex) {
            log.warn("Skipping daily challenge seed due to malformed challenge data: {}", ex.getMostSpecificCause().getMessage());
            return;
        }
        if (challenges.size() < 3) {
            log.warn("Not enough challenges to seed daily challenge (found {}, need 3)", challenges.size());
            return;
        }

        List<String> challengeIds = challenges.stream()
                .map(c -> c.getId().toString())
                .toList();

        DailyChallenge daily = DailyChallenge.builder()
                .challengeDate(today)
                .challengeIds(challengeIds)
                .build();
        dailyChallengeRepository.save(daily);
        log.info("Seeded daily challenge for {} with {} challenges", today, challengeIds.size());
    }
}

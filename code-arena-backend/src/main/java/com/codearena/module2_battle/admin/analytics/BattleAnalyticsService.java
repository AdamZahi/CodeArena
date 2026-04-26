package com.codearena.module2_battle.admin.analytics;

import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module2_battle.admin.analytics.dto.*;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomChallengeRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.module2_battle.repository.BattleSubmissionRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleAnalyticsService {

    private final BattleRoomRepository roomRepository;
    private final BattleParticipantRepository participantRepository;
    private final BattleRoomChallengeRepository roomChallengeRepository;
    private final BattleSubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "battleSummary", condition = "!#refresh")
    public BattleSummaryDTO getSummary(boolean refresh) {
        long total = roomRepository.count();
        long active = roomRepository.countByStatus(BattleRoomStatus.IN_PROGRESS);
        long completed = roomRepository.countByStatus(BattleRoomStatus.FINISHED);
        long cancelled = roomRepository.countByStatus(BattleRoomStatus.CANCELLED);
        Double avg = roomRepository.averageDurationMinutes();
        long globalWins = participantRepository.countGlobalWins();
        long globalSlots = participantRepository.countGlobalFinishedSlots();
        double winRate = globalSlots == 0 ? 0.0 : (double) globalWins / (double) globalSlots;
        long totalParticipants = participantRepository.countDistinctParticipants();
        return new BattleSummaryDTO(
                total,
                active,
                completed,
                cancelled,
                avg == null ? 0.0 : avg,
                round2(winRate),
                totalParticipants
        );
    }

    @Cacheable(value = "battleTimeline", key = "#from.toString() + '-' + #to.toString()", condition = "!#refresh")
    public List<BattleTimelineDTO> getTimeline(LocalDateTime from, LocalDateTime to, boolean refresh) {
        Instant fromI = from.atZone(ZoneId.systemDefault()).toInstant();
        Instant toI = to.atZone(ZoneId.systemDefault()).toInstant();
        List<Object[]> rows = roomRepository.timelineByDay(fromI, toI);
        Map<LocalDate, Long> byDay = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate day;
            Object dayCol = row[0];
            if (dayCol instanceof Date d) {
                day = d.toLocalDate();
            } else if (dayCol instanceof java.time.LocalDate ld) {
                day = ld;
            } else {
                day = LocalDate.parse(dayCol.toString());
            }
            byDay.put(day, ((Number) row[1]).longValue());
        }
        // Fill gaps so charts don't have missing days
        List<BattleTimelineDTO> out = new ArrayList<>();
        LocalDate cursor = from.toLocalDate();
        LocalDate end = to.toLocalDate();
        while (!cursor.isAfter(end)) {
            out.add(new BattleTimelineDTO(cursor.toString(), byDay.getOrDefault(cursor, 0L)));
            cursor = cursor.plusDays(1);
        }
        return out;
    }

    @Cacheable(value = "topChallenges", key = "#limit", condition = "!#refresh")
    public List<TopChallengeDTO> getTopChallenges(int limit, boolean refresh) {
        List<Object[]> rows = roomChallengeRepository.findTopChallengesByUsage(PageRequest.of(0, Math.max(1, limit)));
        if (rows.isEmpty()) return List.of();

        List<Long> challengeIds = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            try {
                challengeIds.add(Long.parseLong(String.valueOf(r[0]).trim()));
            } catch (NumberFormatException ignored) { /* skip non-numeric */ }
        }
        Map<Long, String> titleById = new HashMap<>();
        Map<Long, String> difficultyById = new HashMap<>();
        if (!challengeIds.isEmpty()) {
            challengeRepository.findByIdsSanitized(challengeIds).forEach(row -> {
                Long id = row[0] == null ? null : ((Number) row[0]).longValue();
                if (id != null) {
                    titleById.put(id, row[1] == null ? null : String.valueOf(row[1]));
                    difficultyById.put(id, row[3] == null ? null : String.valueOf(row[3]));
                }
            });
        }

        List<TopChallengeDTO> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            String idStr = String.valueOf(r[0]).trim();
            long count = ((Number) r[1]).longValue();
            String title = "Unknown #" + idStr;
            String difficulty = null;
            try {
                Long id = Long.parseLong(idStr);
                title = titleById.getOrDefault(id, title);
                difficulty = difficultyById.get(id);
            } catch (NumberFormatException ignored) { }
            out.add(new TopChallengeDTO(idStr, title, count, difficulty));
        }
        return out;
    }

    @Cacheable(value = "topPlayers", key = "#limit", condition = "!#refresh")
    public List<TopPlayerDTO> getTopPlayers(int limit, boolean refresh) {
        List<Object[]> rows = participantRepository.findTopPlayersByWins(PageRequest.of(0, Math.max(1, limit)));
        if (rows.isEmpty()) return List.of();

        List<TopPlayerDTO> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            String userId = (String) r[0];
            long played = ((Number) r[1]).longValue();
            long won = ((Number) r[2]).longValue();
            double winRate = played == 0 ? 0.0 : (double) won / (double) played;

            String username = userId;
            long xpEarned = 0;
            User user = userRepository.findByAuth0Id(userId).orElse(null);
            if (user != null) {
                username = pickDisplayName(user);
                xpEarned = user.getTotalXp() == null ? 0L : user.getTotalXp();
            }
            out.add(new TopPlayerDTO(userId, username, played, won, round2(winRate), xpEarned));
        }
        return out;
    }

    @Cacheable(value = "languageDistribution", key = "#from.toString() + '-' + #to.toString()", condition = "!#refresh")
    public List<LanguageDistributionDTO> getLanguageDistribution(LocalDateTime from, LocalDateTime to, boolean refresh) {
        List<Object[]> rows = submissionRepository.countGroupedByLanguageBetween(from, to);
        long total = 0;
        for (Object[] r : rows) total += ((Number) r[1]).longValue();
        if (total == 0) return List.of();

        List<LanguageDistributionDTO> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            String lang = String.valueOf(r[0]);
            long count = ((Number) r[1]).longValue();
            out.add(new LanguageDistributionDTO(lang, count, round2((double) count / (double) total)));
        }
        return out;
    }

    @Cacheable(value = "outcomeDistribution", condition = "!#refresh")
    public OutcomeDistributionDTO getOutcomeDistribution(boolean refresh) {
        long wins = participantRepository.countGlobalWins();
        long slots = participantRepository.countGlobalFinishedSlots();
        long abandoned = roomRepository.countByStatus(BattleRoomStatus.CANCELLED);
        // "Draws" = finished player slots that did NOT win. Conservative interpretation
        // because the schema has no explicit draw flag.
        long draws = Math.max(0L, slots - wins);
        long denom = wins + draws + abandoned;
        if (denom == 0) {
            return new OutcomeDistributionDTO(0, 0, 0, 0.0, 0.0, 0.0);
        }
        return new OutcomeDistributionDTO(
                wins, draws, abandoned,
                round2((double) wins / (double) denom),
                round2((double) draws / (double) denom),
                round2((double) abandoned / (double) denom)
        );
    }

    @Cacheable(value = "avgDuration", condition = "!#refresh")
    public AvgDurationDTO getAverageDuration(boolean refresh) {
        Double avg = roomRepository.averageDurationMinutes();
        long sample = roomRepository.countFinishedWithDurations();
        return new AvgDurationDTO(avg == null ? 0.0 : round2(avg), sample);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String pickDisplayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) return user.getNickname();
        StringBuilder sb = new StringBuilder();
        if (user.getFirstName() != null) sb.append(user.getFirstName());
        if (user.getLastName() != null) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(user.getLastName());
        }
        if (sb.length() > 0) return sb.toString();
        if (user.getEmail() != null) return user.getEmail();
        return user.getAuth0Id();
    }
}

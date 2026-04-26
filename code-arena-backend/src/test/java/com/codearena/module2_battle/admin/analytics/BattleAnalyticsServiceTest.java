package com.codearena.module2_battle.admin.analytics;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module2_battle.admin.analytics.dto.*;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomChallengeRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.module2_battle.repository.BattleSubmissionRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleAnalyticsServiceTest {

    @Mock private BattleRoomRepository roomRepository;
    @Mock private BattleParticipantRepository participantRepository;
    @Mock private BattleRoomChallengeRepository roomChallengeRepository;
    @Mock private BattleSubmissionRepository submissionRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private BattleAnalyticsService service;

    @Test
    void summary_computesGlobalKpis() {
        when(roomRepository.count()).thenReturn(50L);
        when(roomRepository.countByStatus(BattleRoomStatus.IN_PROGRESS)).thenReturn(3L);
        when(roomRepository.countByStatus(BattleRoomStatus.FINISHED)).thenReturn(40L);
        when(roomRepository.countByStatus(BattleRoomStatus.CANCELLED)).thenReturn(5L);
        when(roomRepository.averageDurationMinutes()).thenReturn(22.5);
        when(participantRepository.countGlobalWins()).thenReturn(40L);
        when(participantRepository.countGlobalFinishedSlots()).thenReturn(80L);
        when(participantRepository.countDistinctParticipants()).thenReturn(120L);

        BattleSummaryDTO dto = service.getSummary(false);

        assertThat(dto.totalBattles()).isEqualTo(50L);
        assertThat(dto.activeBattles()).isEqualTo(3L);
        assertThat(dto.completedBattles()).isEqualTo(40L);
        assertThat(dto.abandonedBattles()).isEqualTo(5L);
        assertThat(dto.avgDurationMinutes()).isEqualTo(22.5);
        assertThat(dto.globalWinRate()).isEqualTo(0.5);
        assertThat(dto.totalParticipants()).isEqualTo(120L);
    }

    @Test
    void timeline_fillsGapsForMissingDays() {
        LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 3, 23, 59);
        java.sql.Date day = java.sql.Date.valueOf("2026-04-02");
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{day, 5L});
        when(roomRepository.timelineByDay(any(), any())).thenReturn(rows);

        List<BattleTimelineDTO> result = service.getTimeline(from, to, false);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).count()).isZero();
        assertThat(result.get(1).count()).isEqualTo(5L);
        assertThat(result.get(2).count()).isZero();
    }

    @Test
    void topPlayers_resolvesUsernameAndWinRate() {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{"auth0|alice", 10L, 7L});
        when(participantRepository.findTopPlayersByWins(any())).thenReturn(rows);
        User alice = User.builder().auth0Id("auth0|alice").nickname("Alice").totalXp(1234L).build();
        when(userRepository.findByAuth0Id("auth0|alice")).thenReturn(Optional.of(alice));

        List<TopPlayerDTO> top = service.getTopPlayers(5, false);

        assertThat(top).hasSize(1);
        assertThat(top.get(0).username()).isEqualTo("Alice");
        assertThat(top.get(0).winRate()).isEqualTo(0.7);
        assertThat(top.get(0).xpEarned()).isEqualTo(1234L);
    }

    @Test
    void topChallenges_joinsTitleAndDifficulty() {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{"42", 11L});
        when(roomChallengeRepository.findTopChallengesByUsage(any())).thenReturn(rows);
        java.util.List<Object[]> challengeRows = new java.util.ArrayList<>();
        challengeRows.add(new Object[]{42L, "Two Sum", null, "EASY", null, null, null, null});
        when(challengeRepository.findByIdsSanitized(List.of(42L))).thenReturn(challengeRows);

        List<TopChallengeDTO> top = service.getTopChallenges(5, false);

        assertThat(top).hasSize(1);
        assertThat(top.get(0).title()).isEqualTo("Two Sum");
        assertThat(top.get(0).difficulty()).isEqualTo("EASY");
        assertThat(top.get(0).timesUsed()).isEqualTo(11L);
    }

    @Test
    void languageDistribution_computesPercentages() {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{"java", 6L});
        rows.add(new Object[]{"python", 4L});
        when(submissionRepository.countGroupedByLanguageBetween(any(), any())).thenReturn(rows);

        List<LanguageDistributionDTO> dist = service.getLanguageDistribution(
            LocalDateTime.now().minusDays(7), LocalDateTime.now(), false);

        assertThat(dist).hasSize(2);
        assertThat(dist.get(0).percentage()).isEqualTo(0.6);
        assertThat(dist.get(1).percentage()).isEqualTo(0.4);
    }

    @Test
    void outcomeDistribution_handlesEmptyCleanly() {
        when(participantRepository.countGlobalWins()).thenReturn(0L);
        when(participantRepository.countGlobalFinishedSlots()).thenReturn(0L);
        when(roomRepository.countByStatus(BattleRoomStatus.CANCELLED)).thenReturn(0L);

        OutcomeDistributionDTO dto = service.getOutcomeDistribution(false);

        assertThat(dto.wins()).isZero();
        assertThat(dto.draws()).isZero();
        assertThat(dto.abandoned()).isZero();
        assertThat(dto.winRate()).isZero();
    }

    @Test
    void avgDuration_returnsZeroWhenNoSamples() {
        when(roomRepository.averageDurationMinutes()).thenReturn(null);
        when(roomRepository.countFinishedWithDurations()).thenReturn(0L);

        AvgDurationDTO dto = service.getAverageDuration(false);

        assertThat(dto.avgDurationMinutes()).isZero();
        assertThat(dto.sampleSize()).isZero();
    }
}

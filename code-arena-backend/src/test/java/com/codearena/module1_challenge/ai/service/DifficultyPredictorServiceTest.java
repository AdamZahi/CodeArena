package com.codearena.module1_challenge.ai.service;

import com.codearena.module1_challenge.ai.entity.ChallengeDifficultyProfile;
import com.codearena.module1_challenge.ai.repository.ChallengeDifficultyProfileRepository;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.Submission;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DifficultyPredictorServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private ChallengeDifficultyProfileRepository profileRepository;

    @InjectMocks
    private DifficultyPredictorService predictorService;

    private Challenge challenge;

    @BeforeEach
    void setUp() {
        challenge = Challenge.builder()
                .id(1L)
                .difficulty("MEDIUM")
                .build();
    }

    @Test
    @DisplayName("analyzeChallengeData should set default score when no submissions")
    void shouldSetDefaultOnNoSubmissions() {
        when(submissionRepository.findByChallengeIdOrderBySubmittedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(profileRepository.findByChallengeId(1L)).thenReturn(Optional.empty());

        predictorService.analyzeChallengeData(challenge);

        ArgumentCaptor<ChallengeDifficultyProfile> captor = ArgumentCaptor.forClass(ChallengeDifficultyProfile.class);
        verify(profileRepository).save(captor.capture());
        
        // MEDIUM default is 50.0
        assertThat(captor.getValue().getAiDifficultyScore()).isEqualTo(50.0f);
        assertThat(captor.getValue().getSampleSize()).isEqualTo(0);
    }

    @Test
    @DisplayName("analyzeChallengeData should calculate harder score for low pass rate and high TLE")
    void shouldCalculateHarderScore() {
        // 5 submissions: 1 ACCEPTED, 2 TLE, 2 WA. From 2 unique users.
        Submission s1 = Submission.builder().userId("u1").status("ACCEPTED").executionTime(0.5f).build();
        Submission s2 = Submission.builder().userId("u1").status("TIME_LIMIT_EXCEEDED").build();
        Submission s3 = Submission.builder().userId("u2").status("TIME_LIMIT_EXCEEDED").build();
        Submission s4 = Submission.builder().userId("u2").status("WRONG_ANSWER").build();
        Submission s5 = Submission.builder().userId("u2").status("WRONG_ANSWER").build();
        
        when(submissionRepository.findByChallengeIdOrderBySubmittedAtDesc(1L)).thenReturn(List.of(s1, s2, s3, s4, s5));
        when(profileRepository.findByChallengeId(1L)).thenReturn(Optional.empty());

        predictorService.analyzeChallengeData(challenge);

        ArgumentCaptor<ChallengeDifficultyProfile> captor = ArgumentCaptor.forClass(ChallengeDifficultyProfile.class);
        verify(profileRepository).save(captor.capture());
        
        ChallengeDifficultyProfile saved = captor.getValue();
        assertThat(saved.getSampleSize()).isEqualTo(5);
        assertThat(saved.getPassRate()).isEqualTo(0.2f); // 1/5
        assertThat(saved.getAvgAttempts()).isEqualTo(2.5f); // 5 total / 2 users
        assertThat(saved.getTleRate()).isEqualTo(0.4f); // 2/5
        
        // Base (Medium=50) + PassRateAdj (0.5-0.2)*40=12 + AttemptsAdj (2.5-1)*5=7.5 + TleAdj 0.4*30=12
        // 50 + 12 + 7.5 + 12 = 81.5
        assertThat(saved.getAiDifficultyScore()).isEqualTo(81.5f);
    }

    @Test
    @DisplayName("recalculateAll should iterate over all challenges")
    void shouldProcessAllChallenges() {
        Challenge c2 = Challenge.builder().id(2L).difficulty("EASY").build();
        when(challengeRepository.findAll()).thenReturn(List.of(challenge, c2));
        
        // Use spy or just verify secondary calls
        predictorService.recalculateAll();
        
        verify(submissionRepository, times(1)).findByChallengeIdOrderBySubmittedAtDesc(1L);
        verify(submissionRepository, times(1)).findByChallengeIdOrderBySubmittedAtDesc(2L);
    }
}

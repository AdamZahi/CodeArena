package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.SubmissionDto;
import com.codearena.module1_challenge.dto.SubmitCodeRequest;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.Submission;
import com.codearena.module1_challenge.entity.TestCase;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import com.codearena.module1_challenge.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private ExecutionService executionService;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private Challenge challenge;
    private Submission submission;
    private final Long CHALLENGE_ID = 1L;
    private final String USER_ID = "auth0|user123";

    @BeforeEach
    void setUp() {
        challenge = Challenge.builder()
                .id(CHALLENGE_ID)
                .title("Two Sum")
                .difficulty("EASY")
                .build();

        submission = Submission.builder()
                .id(100L)
                .challenge(challenge)
                .userId(USER_ID)
                .code("System.out.println(42);")
                .language("JAVA")
                .status("PENDING")
                .submittedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("submitCode")
    class SubmitCode {

        @Test
        @DisplayName("should create a submission, load test cases, and trigger async execution")
        void shouldSubmitCode() {
            SubmitCodeRequest request = SubmitCodeRequest.builder()
                    .challengeId(CHALLENGE_ID)
                    .code("int x = 1;")
                    .language("JAVA")
                    .build();

            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
            when(submissionRepository.save(any(Submission.class))).thenReturn(submission);
            List<Object[]> tcRows = new ArrayList<>();
            tcRows.add(new Object[]{"[2,7]", "9", false});
            when(testCaseRepository.findRawByNumericChallengeId(CHALLENGE_ID))
                    .thenReturn(tcRows);

            SubmissionDto result = submissionService.submitCode(request, USER_ID);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getChallengeTitle()).isEqualTo("Two Sum");

            // Verify submission was saved with correct fields
            ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
            verify(submissionRepository).save(captor.capture());
            assertThat(captor.getValue().getLanguage()).isEqualTo("JAVA");
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);

            // Verify async execution was triggered
            verify(executionService).executeSubmission(eq(submission), anyList());
        }

        @Test
        @DisplayName("should throw when challenge not found")
        void shouldThrowWhenChallengeNotFound() {
            SubmitCodeRequest request = SubmitCodeRequest.builder()
                    .challengeId(999L).code("x").language("JAVA").build();

            when(challengeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> submissionService.submitCode(request, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Challenge not found: 999");
        }
    }

    @Nested
    @DisplayName("getSubmissionStatus")
    class GetSubmissionStatus {

        @Test
        @DisplayName("should return the submission DTO for a valid ID")
        void shouldReturnSubmission() {
            when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));

            SubmissionDto result = submissionService.getSubmissionStatus(100L);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getChallengeId()).isEqualTo(CHALLENGE_ID);
        }

        @Test
        @DisplayName("should throw when submission not found")
        void shouldThrowWhenNotFound() {
            when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> submissionService.getSubmissionStatus(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Submission not found: 999");
        }
    }

    @Nested
    @DisplayName("getUserSubmissions")
    class GetUserSubmissions {

        @Test
        @DisplayName("should return all submissions for a given user")
        void shouldReturnUserSubmissions() {
            when(submissionRepository.findByUserIdOrderBySubmittedAtDesc(USER_ID))
                    .thenReturn(List.of(submission));

            List<SubmissionDto> result = submissionService.getUserSubmissions(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should return empty list when user has no submissions")
        void shouldReturnEmptyList() {
            when(submissionRepository.findByUserIdOrderBySubmittedAtDesc("unknown"))
                    .thenReturn(Collections.emptyList());

            List<SubmissionDto> result = submissionService.getUserSubmissions("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getChallengeSubmissions")
    class GetChallengeSubmissions {

        @Test
        @DisplayName("should return all submissions for a given challenge")
        void shouldReturnChallengeSubmissions() {
            when(submissionRepository.findByChallengeIdOrderBySubmittedAtDesc(CHALLENGE_ID))
                    .thenReturn(List.of(submission));

            List<SubmissionDto> result = submissionService.getChallengeSubmissions(CHALLENGE_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChallengeId()).isEqualTo(CHALLENGE_ID);
        }
    }
}

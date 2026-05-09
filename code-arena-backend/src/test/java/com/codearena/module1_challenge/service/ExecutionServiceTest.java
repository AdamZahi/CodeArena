package com.codearena.module1_challenge.service;

import com.codearena.execution.CodeExecutionService;
import com.codearena.execution.ExecutionRequest;
import com.codearena.execution.ExecutionResult;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.Submission;
import com.codearena.module1_challenge.entity.TestCase;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import com.codearena.user.service.CustomizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    @Mock private CodeExecutionService codeExecutionService;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserRepository userRepository;
    @Mock private XpCalculatorService xpCalculatorService;
    @Mock private CustomizationService customizationService;

    @InjectMocks
    private ExecutionService executionService;

    private Submission submission;
    private Challenge challenge;
    private TestCase testCase;

    @BeforeEach
    void setUp() {
        challenge = Challenge.builder().id(1L).difficulty("MEDIUM").build();
        submission = Submission.builder()
                .id(100L)
                .challenge(challenge)
                .userId("user123")
                .code("print(1)")
                .language("62")
                .status("PENDING")
                .build();
        testCase = new TestCase();
        testCase.setInput("1");
        testCase.setExpectedOutput("1");
    }

    @Test
    @DisplayName("executeSubmission should mark ACCEPTED and award XP when all tests pass")
    void shouldExecuteAndAccept() throws Exception {
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));

        ExecutionResult result = ExecutionResult.builder()
                .stdout("1")
                .stderr("")
                .compileError("")
                .exitCode(0)
                .executionTimeMs(50L)
                .engineUsed("piston")
                .build();
        when(codeExecutionService.execute(any(ExecutionRequest.class))).thenReturn(result);

        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));
        when(xpCalculatorService.calculateXp(eq("MEDIUM"), any())).thenReturn(150);
        User user = User.builder().auth0Id("user123").totalXp(500L).build();
        when(userRepository.findByAuth0Id("user123")).thenReturn(Optional.of(user));

        executionService.executeSubmission(submission, List.of(testCase));

        assertThat(submission.getStatus()).isEqualTo("ACCEPTED");
        assertThat(submission.getXpEarned()).isEqualTo("150");
        assertThat(user.getTotalXp()).isEqualTo(650L);
        verify(submissionRepository, atLeast(2)).save(submission);
        verify(customizationService).checkAndGrantUnlocks("user123");
    }

    @Test
    @DisplayName("executeSubmission should handle WRONG_ANSWER")
    void shouldHandleWrongAnswer() throws Exception {
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));

        ExecutionResult result = ExecutionResult.builder()
                .stdout("2")
                .stderr("")
                .compileError("")
                .exitCode(0)
                .executionTimeMs(50L)
                .engineUsed("piston")
                .build();
        when(codeExecutionService.execute(any(ExecutionRequest.class))).thenReturn(result);

        executionService.executeSubmission(submission, List.of(testCase));

        assertThat(submission.getStatus()).isEqualTo("WRONG_ANSWER");
        verify(userRepository, never()).save(any());
    }
}
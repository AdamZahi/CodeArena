package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.Submission;
import com.codearena.module1_challenge.entity.TestCase;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.SubmissionRepository;
import com.codearena.module2_battle.dto.ComplexityClassificationResult;
import com.codearena.module2_battle.dto.PistonExecutionRequest;
import com.codearena.module2_battle.dto.PistonExecutionResult;
import com.codearena.module2_battle.service.ClassifierBridgeService;
import com.codearena.module2_battle.service.CodeWrapperService;
import com.codearena.module2_battle.service.PistonClient;
import com.codearena.module2_battle.util.PistonLanguageMapper;
import com.codearena.module2_battle.util.PistonLanguageMapper.PistonLang;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import com.codearena.user.service.CustomizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private PistonClient pistonClient;
    @Mock
    private PistonLanguageMapper pistonLanguageMapper;
    @Mock
    private CodeWrapperService codeWrapperService;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private XpCalculatorService xpCalculatorService;
    @Mock
    private CustomizationService customizationService;
    @Mock
    private ClassifierBridgeService classifierBridgeService;

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

        PistonLang pistonLang = new PistonLang("python", "3.12.0");
        ComplexityClassificationResult complexityResult = ComplexityClassificationResult.builder()
                .label("O1")
                .display("O(1)")
                .score(100.0)
                .confidence(0.99)
                .fallback(false)
                .build();

        lenient().when(pistonLanguageMapper.toPistonLang(anyString())).thenReturn(pistonLang);
        lenient().when(codeWrapperService.wrapCode(anyString(), anyString(), anyString())).thenReturn(null);
        lenient().when(classifierBridgeService.classify(anyString(), anyString())).thenReturn(complexityResult);
    }

    @Test
    @DisplayName("executeSubmission should mark ACCEPTED and award XP when all tests pass")
    void shouldExecuteAndAccept() throws Exception {
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));
        when(pistonClient.execute(any(PistonExecutionRequest.class))).thenReturn(
                PistonExecutionResult.builder()
                        .stdout("1")
                        .stderr("")
                        .compileOutput(null)
                        .exitCode(0)
                        .build());
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));
        when(xpCalculatorService.calculateXp(eq("MEDIUM"), any())).thenReturn(150);
        User user = User.builder().auth0Id("user123").totalXp(500L).build();
        when(userRepository.findByAuth0Id("user123")).thenReturn(Optional.of(user));

        executionService.executeSubmission(submission, List.of(testCase));

        assertThat(submission.getStatus()).isEqualTo("ACCEPTED");
        assertThat(submission.getXpEarned()).isEqualTo("150");
        assertThat(user.getTotalXp()).isEqualTo(650L);
        assertThat(user.getLevel()).isEqualTo(2);
        assertThat(submission.getErrorOutput()).contains("Passed");

        ArgumentCaptor<PistonExecutionRequest> requestCaptor = ArgumentCaptor.forClass(PistonExecutionRequest.class);
        verify(pistonClient).execute(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getLanguage()).isEqualTo("python");
        assertThat(requestCaptor.getValue().getVersion()).isEqualTo("3.12.0");
        assertThat(requestCaptor.getValue().getSourceCode()).isEqualTo("print(1)");
        assertThat(requestCaptor.getValue().getStdin()).isEqualTo("1");

        verify(submissionRepository, atLeast(2)).save(submission);
        verify(customizationService).checkAndGrantUnlocks("user123");
    }

    @Test
    @DisplayName("executeSubmission should handle WRONG_ANSWER")
    void shouldHandleWrongAnswer() throws Exception {
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));
        when(pistonClient.execute(any(PistonExecutionRequest.class))).thenReturn(
                PistonExecutionResult.builder()
                        .stdout("2")
                        .stderr("")
                        .compileOutput(null)
                        .exitCode(0)
                        .build());

        executionService.executeSubmission(submission, List.of(testCase));

        assertThat(submission.getStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(submission.getErrorOutput()).contains("Expected: [1] Got: [2]");
        verify(userRepository, never()).save(any());
        verify(pistonClient).execute(any(PistonExecutionRequest.class));
    }
}
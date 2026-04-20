package com.codearena.module2_battle.service;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.TestCase;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.TestCaseRepository;
import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.entity.BattleRoomChallenge;
import com.codearena.module2_battle.entity.BattleSubmission;
import com.codearena.module2_battle.enums.*;
import com.codearena.module2_battle.exception.*;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomChallengeRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.module2_battle.repository.BattleSubmissionRepository;
import com.codearena.module2_battle.util.PistonLanguageMapper;
import com.codearena.module2_battle.util.PistonLanguageMapper.PistonLang;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Core service for the live arena phase. Handles everything from the moment a room
 * becomes IN_PROGRESS until it transitions to FINISHED.
 *
 * Never directly sets any status field on BattleRoom — all status changes go through
 * BattleRoomStateMachineService.
 */
@Slf4j
@Service
public class BattleArenaService {

    private final BattleRoomRepository battleRoomRepository;
    private final BattleParticipantRepository participantRepository;
    private final BattleRoomChallengeRepository roomChallengeRepository;
    private final BattleSubmissionRepository submissionRepository;
    private final ChallengeRepository challengeRepository;
    private final TestCaseRepository testCaseRepository;
    private final UserRepository userRepository;
    private final PistonClient pistonClient;
    private final PistonLanguageMapper languageMapper;
    private final ArenaBroadcastService arenaBroadcastService;
    private final BattleRoomStateMachineService stateMachineService;
    private final com.codearena.module2_battle.config.TimeLimitProperties timeLimitProperties;
    private final CodeWrapperService codeWrapperService;
    private final BattleConnectionTracker connectionTracker;
    private final Executor submissionExecutor;

    // Lock object for thread-safe match completion checks
    private final Object matchCompletionLock = new Object();

    // Rate-limit map for activity events: key = participantId + activityType, value = last sent instant
    private final ConcurrentHashMap<String, Instant> activityRateLimit = new ConcurrentHashMap<>();
    private static final long ACTIVITY_RATE_LIMIT_MS = 2000;

    public BattleArenaService(
            BattleRoomRepository battleRoomRepository,
            BattleParticipantRepository participantRepository,
            BattleRoomChallengeRepository roomChallengeRepository,
            BattleSubmissionRepository submissionRepository,
            ChallengeRepository challengeRepository,
            TestCaseRepository testCaseRepository,
            UserRepository userRepository,
            PistonClient pistonClient,
            PistonLanguageMapper languageMapper,
            ArenaBroadcastService arenaBroadcastService,
            BattleRoomStateMachineService stateMachineService,
            com.codearena.module2_battle.config.TimeLimitProperties timeLimitProperties,
            CodeWrapperService codeWrapperService,
            BattleConnectionTracker connectionTracker,
            @Qualifier("submissionExecutor") Executor submissionExecutor) {
        this.battleRoomRepository = battleRoomRepository;
        this.participantRepository = participantRepository;
        this.roomChallengeRepository = roomChallengeRepository;
        this.submissionRepository = submissionRepository;
        this.challengeRepository = challengeRepository;
        this.testCaseRepository = testCaseRepository;
        this.userRepository = userRepository;
        this.pistonClient = pistonClient;
        this.languageMapper = languageMapper;
        this.arenaBroadcastService = arenaBroadcastService;
        this.stateMachineService = stateMachineService;
        this.timeLimitProperties = timeLimitProperties;
        this.codeWrapperService = codeWrapperService;
        this.connectionTracker = connectionTracker;
        this.submissionExecutor = submissionExecutor;
    }

    // ──────────────────────────────────────────────
    // 3.1 — getArenaState
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ArenaStateResponse getArenaState(String roomId, String requestingUserId) {
        BattleRoom room = loadRoom(roomId);

        if (room.getStatus() != BattleRoomStatus.IN_PROGRESS) {
            throw new ArenaNotActiveException(roomId, room.getStatus());
        }

        // Confirm requesting user is a participant (player or spectator)
        participantRepository.findByRoomIdAndUserId(roomId, requestingUserId)
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));

        List<BattleRoomChallenge> roomChallenges = roomChallengeRepository.findByRoomIdOrderByPositionAsc(roomId);

        List<ArenaChallengeResponse> challengeResponses = roomChallenges.stream()
                .map(rc -> buildArenaChallengeResponse(rc))
                .toList();

        List<BattleParticipant> players = participantRepository.findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);
        List<ArenaParticipantProgressResponse> participantProgress = players.stream()
                .map(p -> buildParticipantProgress(p, room.getChallengeCount()))
                .toList();

        long remainingSeconds = calculateRemainingSeconds(room);

        return ArenaStateResponse.builder()
                .roomId(roomId)
                .status(room.getStatus())
                .challenges(challengeResponses)
                .participants(participantProgress)
                .remainingSeconds(remainingSeconds)
                .build();
    }

    // ──────────────────────────────────────────────
    // 3.2 — submitSolution
    // ──────────────────────────────────────────────

    /**
     * Submits a solution for judging. The submission is persisted immediately with PENDING status
     * and returned to the caller. Piston execution and result processing happen asynchronously
     * on the submissionExecutor thread pool — the HTTP response is never blocked by Piston.
     */
    @Transactional
    public SubmissionResultResponse submitSolution(String userId, SubmitSolutionRequest request) {
        String roomId = request.getRoomId();
        BattleRoom room = loadRoom(roomId);

        if (room.getStatus() != BattleRoomStatus.IN_PROGRESS) {
            throw new ArenaNotActiveException(roomId, room.getStatus());
        }

        BattleParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));

        if (participant.getRole() != ParticipantRole.PLAYER) {
            throw new InvalidParticipantActionException("Spectators cannot submit solutions");
        }

        // Load the room challenge and confirm it belongs to this room
        BattleRoomChallenge roomChallenge = roomChallengeRepository
                .findById(UUID.fromString(request.getRoomChallengeId()))
                .orElseThrow(() -> new BattleRoomNotFoundException(request.getRoomChallengeId()));

        if (!roomChallenge.getRoomId().equals(roomId)) {
            throw new InvalidParticipantActionException("Challenge does not belong to this room");
        }

        // Check if this player already has an ACCEPTED submission for this challenge
        String participantId = participant.getId().toString();
        List<BattleSubmission> existingSubmissions = submissionRepository
                .findByParticipantIdOrderBySubmittedAtAsc(participantId);

        boolean alreadySolved = existingSubmissions.stream()
                .anyMatch(s -> s.getRoomChallengeId().equals(request.getRoomChallengeId())
                        && s.getStatus() == BattleSubmissionStatus.ACCEPTED);
        if (alreadySolved) {
            throw new ChallengeAlreadySolvedException(roomChallenge.getPosition());
        }

        // Compute attempt number for this participant + challenge combination
        int attemptNumber = (int) submissionRepository
                .countByParticipantIdAndRoomChallengeId(participantId, request.getRoomChallengeId()) + 1;

        // Persist submission immediately with PENDING status before calling Piston
        BattleSubmission submission = BattleSubmission.builder()
                .participantId(participantId)
                .roomChallengeId(request.getRoomChallengeId())
                .language(request.getLanguage())
                .code(request.getCode())
                .status(BattleSubmissionStatus.PENDING)
                .attemptNumber(attemptNumber)
                .build();
        submission = submissionRepository.save(submission);

        String submissionId = submission.getId().toString();
        int challengePosition = roomChallenge.getPosition();

        long challengeId = parseNumericChallengeId(roomChallenge.getChallengeId());

        // Load all test cases (including hidden ones) for judging, using sanitized native reads.
        List<TestCase> allTestCases = loadSanitizedTestCases(challengeId);
        if (allTestCases.isEmpty()) {
            throw new InvalidParticipantActionException("Challenge has no valid test cases");
        }

        // Map language to Piston runtime (name + version)
        PistonLang pistonLang = languageMapper.toPistonLang(request.getLanguage());
        String sourceCode = request.getCode();

        // Execute on Piston and process result asynchronously on the submissionExecutor thread pool.
        // Each test case is run as a separate Piston execution.
        String language = request.getLanguage();
        submissionExecutor.execute(() -> processPistonSubmission(
                sourceCode, pistonLang, language, allTestCases, submissionId, roomId, participantId,
                request.getRoomChallengeId(), challengePosition, userId,
                room.getChallengeCount(), attemptNumber
        ));

        return SubmissionResultResponse.builder()
                .submissionId(submissionId)
                .roomChallengeId(request.getRoomChallengeId())
                .status(BattleSubmissionStatus.PENDING.name())
                .attemptNumber(attemptNumber)
                .feedback("Submission received — judging in progress")
                .isAccepted(false)
                .build();
    }

    // ──────────────────────────────────────────────
    // 3.2b — reportActivity (Feature 1)
    // ──────────────────────────────────────────────

    public void reportActivity(String roomId, String userId, ActivityRequest request) {
        BattleRoom room = loadRoom(roomId);
        if (room.getStatus() != BattleRoomStatus.IN_PROGRESS) return;

        BattleParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));
        if (participant.getRole() != ParticipantRole.PLAYER) return;

        // Rate-limit: discard if same participant+type within 2 seconds
        String rateLimitKey = participant.getId() + ":" + request.getType();
        Instant now = Instant.now();
        Instant lastSent = activityRateLimit.get(rateLimitKey);
        if (lastSent != null && now.toEpochMilli() - lastSent.toEpochMilli() < ACTIVITY_RATE_LIMIT_MS) {
            return; // discard duplicate
        }
        activityRateLimit.put(rateLimitKey, now);

        String displayName = resolveUsername(userId);
        OpponentActivityEvent event = OpponentActivityEvent.builder()
                .participantId(participant.getId().toString())
                .displayName(displayName)
                .type(request.getType())
                .challengeId(request.getChallengeId())
                .timestamp(now)
                .build();

        arenaBroadcastService.broadcastActivity(roomId, event);
    }

    // ──────────────────────────────────────────────
    // 3.2c — handleReconnect / handleHeartbeat (Feature 3)
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ArenaStateResponse handleReconnect(String roomId, String userId) {
        BattleRoom room = loadRoom(roomId);
        if (room.getStatus() != BattleRoomStatus.IN_PROGRESS) {
            throw new ArenaNotActiveException(roomId, room.getStatus());
        }

        participantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));

        connectionTracker.onReconnect(roomId, userId);
        return getArenaState(roomId, userId);
    }

    public void handleHeartbeat(String roomId, String userId) {
        connectionTracker.updateHeartbeat(userId);
    }

    /**
     * Async callback: runs each test case as a separate Piston execution (synchronous call),
     * updates the submission record, and broadcasts results to arena subscribers.
     */
    private void processPistonSubmission(
            String sourceCode, PistonLang pistonLang, String language, List<TestCase> testCases,
            String submissionId, String roomId, String participantId,
            String roomChallengeId, int challengePosition, String userId,
            int challengeCount, int attemptNumber) {
        int totalTestCases = testCases.size();
        try {
            BattleSubmissionStatus finalStatus = BattleSubmissionStatus.ACCEPTED;
            Integer totalRuntimeMs = null;
            Integer maxMemoryKb = null;
            String failCompileOutput = null;

            // Feature 2: broadcast initial PENDING state for all test cases
            for (int i = 0; i < totalTestCases; i++) {
                arenaBroadcastService.sendTestCaseProgress(userId, TestCaseProgressEvent.builder()
                        .submissionId(submissionId).testCaseIndex(i).totalTestCases(totalTestCases)
                        .status(com.codearena.module2_battle.enums.TestCaseStatus.PENDING).build());
            }

            for (int tcIndex = 0; tcIndex < testCases.size(); tcIndex++) {
                TestCase tc = testCases.get(tcIndex);

                // Feature 2: mark this test case as RUNNING
                arenaBroadcastService.sendTestCaseProgress(userId, TestCaseProgressEvent.builder()
                        .submissionId(submissionId).testCaseIndex(tcIndex).totalTestCases(totalTestCases)
                        .status(com.codearena.module2_battle.enums.TestCaseStatus.RUNNING).build());

                // Try to wrap the code so that user's hardcoded prints are suppressed
                // and only the function result for THIS test case is printed.
                String wrappedCode = codeWrapperService.wrapCode(sourceCode, language, tc.getInput());
                boolean wrapped = wrappedCode != null;

                PistonExecutionRequest pistonRequest = PistonExecutionRequest.builder()
                        .language(pistonLang.language())
                        .version(pistonLang.version())
                        .sourceCode(wrapped ? wrappedCode : sourceCode)
                        .fileName(pistonFileName(pistonLang.language(), wrapped, sourceCode))
                        .stdin(wrapped ? null : tc.getInput())
                        .build();

                PistonExecutionResult result = pistonClient.execute(pistonRequest);
                BattleSubmissionStatus tcStatus = mapPistonStatus(result, tc.getExpectedOutput());

                // Accumulate runtime and memory
                if (result.getCpuTimeMs() != null) {
                    int ms = result.getCpuTimeMs();
                    totalRuntimeMs = (totalRuntimeMs == null) ? ms : totalRuntimeMs + ms;
                }
                Integer memKb = result.getMemoryKb();
                if (memKb != null) {
                    maxMemoryKb = (maxMemoryKb == null) ? memKb : Math.max(maxMemoryKb, memKb);
                }

                if (tcStatus != BattleSubmissionStatus.ACCEPTED) {
                    log.debug("Piston returned non-accepted status {} for submission {}: stdout=[{}], stderr=[{}], expected=[{}]",
                            tcStatus, submissionId,
                            result.getStdout(), result.getStderr(), tc.getExpectedOutput());
                    finalStatus = tcStatus;
                    failCompileOutput = result.getCompileOutput();

                    // Feature 2: broadcast failure for this test case and ERROR for remaining
                    String errorType = tcStatus.name();
                    arenaBroadcastService.sendTestCaseProgress(userId, TestCaseProgressEvent.builder()
                            .submissionId(submissionId).testCaseIndex(tcIndex).totalTestCases(totalTestCases)
                            .status(com.codearena.module2_battle.enums.TestCaseStatus.FAILED).errorType(errorType).build());
                    for (int rem = tcIndex + 1; rem < totalTestCases; rem++) {
                        arenaBroadcastService.sendTestCaseProgress(userId, TestCaseProgressEvent.builder()
                                .submissionId(submissionId).testCaseIndex(rem).totalTestCases(totalTestCases)
                                .status(com.codearena.module2_battle.enums.TestCaseStatus.ERROR).errorType(errorType).build());
                    }
                    break;
                }

                // Feature 2: broadcast PASSED for this test case
                arenaBroadcastService.sendTestCaseProgress(userId, TestCaseProgressEvent.builder()
                        .submissionId(submissionId).testCaseIndex(tcIndex).totalTestCases(totalTestCases)
                        .status(com.codearena.module2_battle.enums.TestCaseStatus.PASSED).build());
            }

            String feedback = buildFeedback(finalStatus, totalRuntimeMs, failCompileOutput, totalTestCases);

            // Update the submission record
            BattleSubmission submission = submissionRepository.findById(UUID.fromString(submissionId)).orElse(null);
            if (submission != null) {
                submission.setStatus(finalStatus);
                submission.setRuntimeMs(totalRuntimeMs);
                submission.setMemoryKb(maxMemoryKb);
                submissionRepository.save(submission);
            }

            boolean isAccepted = finalStatus == BattleSubmissionStatus.ACCEPTED;

            // Send result to the submitting player only
            SubmissionResultResponse resultResponse = SubmissionResultResponse.builder()
                    .submissionId(submissionId)
                    .roomChallengeId(roomChallengeId)
                    .status(finalStatus.name())
                    .attemptNumber(attemptNumber)
                    .runtimeMs(totalRuntimeMs)
                    .memoryKb(maxMemoryKb)
                    .feedback(feedback)
                    .isAccepted(isAccepted)
                    .build();
            arenaBroadcastService.sendSubmissionResult(userId, resultResponse);

            // Build and broadcast opponent progress to all other players
            BattleParticipant participant = participantRepository.findById(UUID.fromString(participantId)).orElse(null);
            if (participant != null) {
                ArenaParticipantProgressResponse progress = buildParticipantProgress(participant, challengeCount);
                OpponentProgressEvent progressEvent = OpponentProgressEvent.builder()
                        .participantId(participantId)
                        .userId(userId)
                        .challengesCompleted(progress.getChallengesCompleted())
                        .currentChallengePosition(progress.getCurrentChallengePosition())
                        .totalAttempts(progress.getTotalAttempts())
                        .isFinished(progress.isFinished())
                        .pulse(isAccepted ? ProgressPulse.ACCEPTED : ProgressPulse.FAILED)
                        .build();
                arenaBroadcastService.broadcastOpponentProgress(roomId, userId, progressEvent);

                // Broadcast spectator feed with 30-second delay
                String username = resolveUsername(userId);
                SpectatorFeedEvent spectatorEvent = SpectatorFeedEvent.builder()
                        .participantId(participantId)
                        .username(username)
                        .challengePosition(challengePosition)
                        .submissionStatus(finalStatus)
                        .attemptNumber(attemptNumber)
                        .delayedAtTimestamp(System.currentTimeMillis())
                        .build();
                arenaBroadcastService.broadcastSpectatorFeedDelayed(roomId, spectatorEvent);

                // If accepted, check if this player has solved all challenges.
                // Wrapped in its own try-catch so a scoring/transition failure doesn't
                // overwrite the already-persisted ACCEPTED submission.
                if (isAccepted && progress.getChallengesCompleted() >= challengeCount) {
                    try {
                        checkMatchCompletion(roomId);
                    } catch (Exception ex) {
                        log.error("Failed to process match completion for room {}: {}", roomId, ex.getMessage(), ex);
                    }
                }
            }

        } catch (CodeExecutionUnavailableException e) {
            log.error("Piston unavailable for submission {}: {}", submissionId, e.getMessage());
            failSubmissionAndNotify(userId, submissionId, roomChallengeId, attemptNumber,
                    BattleSubmissionStatus.COMPILE_ERROR,
                    "Code execution service is temporarily unavailable - please retry");
        } catch (Exception e) {
            log.error("Unexpected error processing submission {}: {}", submissionId, e.getMessage(), e);
            failSubmissionAndNotify(userId, submissionId, roomChallengeId, attemptNumber,
                    BattleSubmissionStatus.RUNTIME_ERROR,
                    "Submission processing failed unexpectedly - please retry");
        }
    }

    private void failSubmissionAndNotify(String userId,
                                         String submissionId,
                                         String roomChallengeId,
                                         int attemptNumber,
                                         BattleSubmissionStatus status,
                                         String feedback) {
        BattleSubmission submission = submissionRepository.findById(UUID.fromString(submissionId)).orElse(null);
        if (submission != null) {
            submission.setStatus(status);
            submissionRepository.save(submission);
        }

        SubmissionResultResponse errorResponse = SubmissionResultResponse.builder()
                .submissionId(submissionId)
                .roomChallengeId(roomChallengeId)
                .status(status.name())
                .attemptNumber(attemptNumber)
                .feedback(feedback)
                .isAccepted(false)
                .build();
        arenaBroadcastService.sendSubmissionResult(userId, errorResponse);
    }

    /**
     * Returns the filename Piston should use for this submission. Only matters for Java
     * (Piston derives the main-class name from the filename). The wrapper always emits
     * {@code class Main}, so wrapped Java goes to Main.java; unwrapped Java uses the
     * detected class name.
     */
    private String pistonFileName(String pistonLanguage, boolean wrapped, String userCode) {
        if (!"java".equals(pistonLanguage)) return null;
        if (wrapped) return "Main.java";
        String detected = CodeWrapperService.detectJavaClass(userCode);
        return (detected != null) ? detected + ".java" : "Main.java";
    }

    /**
     * Maps a Piston execution result into our BattleSubmissionStatus.
     * Piston returns exit code, signal, compile stage output, and run stage stdout/stderr.
     * Precedence: compile error → time-limit (SIGKILL) → runtime error → wrong answer → accepted.
     */
    private BattleSubmissionStatus mapPistonStatus(PistonExecutionResult result, String expectedOutput) {
        if (result == null) {
            return BattleSubmissionStatus.RUNTIME_ERROR;
        }

        // Top-level Piston error (e.g., invalid language/version) is surfaced as errorMessage.
        if (result.getErrorMessage() != null) {
            return BattleSubmissionStatus.COMPILE_ERROR;
        }

        // Compile stage failure (compiled languages only)
        if (result.getCompileOutput() != null && !result.getCompileOutput().isBlank()) {
            return BattleSubmissionStatus.COMPILE_ERROR;
        }

        // Process was killed (typically SIGKILL on timeout/OOM)
        String signal = result.getSignal();
        if (signal != null && !signal.isBlank()) {
            if ("SIGKILL".equals(signal) || "SIGXCPU".equals(signal)) {
                return BattleSubmissionStatus.TIME_LIMIT;
            }
            return BattleSubmissionStatus.RUNTIME_ERROR;
        }

        // Non-zero exit code = runtime error
        if (result.getExitCode() != 0) {
            return BattleSubmissionStatus.RUNTIME_ERROR;
        }

        // Exit 0 — compare output
        if (!outputsMatch(expectedOutput, result.getStdout())) {
            return BattleSubmissionStatus.WRONG_ANSWER;
        }

        return BattleSubmissionStatus.ACCEPTED;
    }

    /**
     * Builds a user-facing feedback string based on submission status.
     * Does not reveal which specific test case failed to prevent gaming.
     */
    private String buildFeedback(BattleSubmissionStatus status, Integer runtimeMs,
                                 String compileOutput, int totalTestCases) {
        return switch (status) {
            case ACCEPTED -> "All " + totalTestCases + " test cases passed";
            case WRONG_ANSWER -> "Wrong answer - verify exact output format (extra prints/spaces/newlines can fail)";
            case TIME_LIMIT -> "Time limit exceeded (" + (runtimeMs != null ? runtimeMs + "ms" : "unknown") + ")";
            case RUNTIME_ERROR -> "Runtime error — check for null references or array bounds";
            case COMPILE_ERROR -> {
                String msg = "Compilation failed";
                if (compileOutput != null && !compileOutput.isBlank()) {
                    String firstLine = compileOutput.lines().findFirst().orElse("");
                    if (!firstLine.isBlank()) {
                        msg += " — " + firstLine;
                    }
                }
                yield msg;
            }
            default -> "Processing";
        };
    }

    private boolean outputsMatch(String expectedOutput, String actualOutput) {
        String normalizedExpected = normalizeOutput(expectedOutput);
        String normalizedActual = normalizeOutput(actualOutput);
        return normalizedExpected.equals(normalizedActual);
    }

    private String normalizeOutput(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.replace("\r\n", "\n").replace("\r", "\n");
        List<String> lines = new ArrayList<>(Arrays.asList(normalized.split("\n", -1)));

        for (int i = 0; i < lines.size(); i++) {
            lines.set(i, lines.get(i).stripTrailing());
        }

        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        return String.join("\n", lines);
    }

    // ──────────────────────────────────────────────
    // 3.3 — checkMatchCompletion
    // ──────────────────────────────────────────────

    /**
     * Called after every ACCEPTED submission. Uses synchronized block to prevent
     * race conditions when two players finish simultaneously — ensures transitionToFinished
     * is called at most once.
     */
    private void checkMatchCompletion(String roomId) {
        synchronized (matchCompletionLock) {
            BattleRoom room = loadRoom(roomId);
            if (room.getStatus() != BattleRoomStatus.IN_PROGRESS) {
                return; // Already finished or cancelled — race condition guard
            }

            List<BattleParticipant> players = participantRepository
                    .findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);

            long finishedCount = players.stream()
                    .filter(p -> isPlayerFinished(p, room.getChallengeCount()))
                    .count();

            if (finishedCount == players.size()) {
                log.info("All {} players finished in room {} — transitioning to FINISHED", players.size(), roomId);
                stateMachineService.transitionToFinished(roomId);
            }
        }
    }

    // ──────────────────────────────────────────────
    // 3.4 — getSubmissionsForParticipant
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SubmissionResultResponse> getSubmissionsForParticipant(
            String roomId, String participantId, String requestingUserId) {
        // Confirm requesting user is either the participant owner or a spectator in the room
        BattleParticipant participant = participantRepository.findById(UUID.fromString(participantId))
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));

        if (!participant.getUserId().equals(requestingUserId)) {
            BattleParticipant requester = participantRepository.findByRoomIdAndUserId(roomId, requestingUserId)
                    .orElseThrow(() -> new ParticipantNotFoundException(roomId));
            if (requester.getRole() != ParticipantRole.SPECTATOR) {
                throw new InvalidParticipantActionException("Only the participant or a spectator can view submissions");
            }
        }

        List<BattleSubmission> submissions = submissionRepository
                .findByParticipantIdOrderBySubmittedAtAsc(participantId);

        return submissions.stream()
                .map(s -> SubmissionResultResponse.builder()
                        .submissionId(s.getId().toString())
                        .roomChallengeId(s.getRoomChallengeId())
                        .status(s.getStatus().name())
                        .attemptNumber(s.getAttemptNumber())
                        .runtimeMs(s.getRuntimeMs())
                        .memoryKb(s.getMemoryKb())
                        .feedback(null)
                        .isAccepted(s.getStatus() == BattleSubmissionStatus.ACCEPTED)
                        .build())
                .toList();
    }

    /**
     * Returns ordered challenges for a room, exposing only visible (non-hidden) test cases.
     */
    @Transactional(readOnly = true)
    public List<ArenaChallengeResponse> getRoomChallenges(String roomId, String requestingUserId) {
        BattleRoom room = loadRoom(roomId);

        if (room.getStatus() != BattleRoomStatus.IN_PROGRESS) {
            throw new ArenaNotActiveException(roomId, room.getStatus());
        }

        participantRepository.findByRoomIdAndUserId(roomId, requestingUserId)
                .orElseThrow(() -> new ParticipantNotFoundException(roomId));

        List<BattleRoomChallenge> roomChallenges = roomChallengeRepository.findByRoomIdOrderByPositionAsc(roomId);
        return roomChallenges.stream()
                .map(this::buildArenaChallengeResponse)
                .toList();
    }

    // ──────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────

    private BattleRoom loadRoom(String roomId) {
        return battleRoomRepository.findById(UUID.fromString(roomId))
                .orElseThrow(() -> new BattleRoomNotFoundException(roomId));
    }

    /**
     * Builds an ArenaChallengeResponse, exposing only non-hidden test cases.
     * Hidden test cases are used by the execution engine for judging but never appear in any response DTO.
     */
    private ArenaChallengeResponse buildArenaChallengeResponse(BattleRoomChallenge rc) {
        long challengeId;
        try {
            challengeId = parseNumericChallengeId(rc.getChallengeId());
        } catch (InvalidParticipantActionException ex) {
            return ArenaChallengeResponse.builder()
                    .roomChallengeId(rc.getId().toString())
                    .position(rc.getPosition())
                    .challengeId(rc.getChallengeId())
                    .build();
        }

        Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
        if (challenge == null) {
            return ArenaChallengeResponse.builder()
                    .roomChallengeId(rc.getId().toString())
                    .position(rc.getPosition())
                    .challengeId(rc.getChallengeId())
                    .build();
        }

        // Only expose non-hidden test cases from sanitized reads.
        List<VisibleTestCaseResponse> visibleTests = loadSanitizedTestCases(challengeId).stream()
                .filter(tc -> !Boolean.TRUE.equals(tc.getIsHidden()))
                .map(tc -> VisibleTestCaseResponse.builder()
                        .input(tc.getInput())
                        .expectedOutput(tc.getExpectedOutput())
                        .build())
                .toList();

        return ArenaChallengeResponse.builder()
                .roomChallengeId(rc.getId().toString())
                .position(rc.getPosition())
                .challengeId(rc.getChallengeId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .difficulty(challenge.getDifficulty())
                .tags(challenge.getTags())
                .visibleTestCases(visibleTests)
                .build();
    }

    private long parseNumericChallengeId(String challengeId) {
        try {
            return Long.parseLong(challengeId);
        } catch (NumberFormatException ex) {
            throw new InvalidParticipantActionException("Invalid challenge reference in room data");
        }
    }

    private List<TestCase> loadSanitizedTestCases(long challengeId) {
        return testCaseRepository.findRawByNumericChallengeId(challengeId).stream()
                .map(row -> TestCase.builder()
                        .input((String) row[0])
                        .expectedOutput((String) row[1])
                        .isHidden(parseBoolean(row[2]))
                        .build())
                .toList();
    }

    private boolean parseBoolean(Object rawValue) {
        if (rawValue == null) {
            return false;
        }
        if (rawValue instanceof Boolean value) {
            return value;
        }
        if (rawValue instanceof Number value) {
            return value.intValue() != 0;
        }
        if (rawValue instanceof byte[] value && value.length > 0) {
            return value[0] != 0;
        }
        return "1".equals(rawValue.toString()) || "true".equalsIgnoreCase(rawValue.toString());
    }

    /**
     * Builds progress info for a participant by analyzing their submissions.
     * challengesCompleted = count of distinct roomChallengeIds with ACCEPTED status.
     * currentChallengePosition = challengesCompleted + 1 (next challenge to work on).
     * totalAttempts = sum of all submissions for this participant.
     */
    private ArenaParticipantProgressResponse buildParticipantProgress(
            BattleParticipant participant, int challengeCount) {
        String participantId = participant.getId().toString();
        List<BattleSubmission> submissions = submissionRepository
                .findByParticipantIdOrderBySubmittedAtAsc(participantId);

        long challengesCompleted = submissions.stream()
                .filter(s -> s.getStatus() == BattleSubmissionStatus.ACCEPTED)
                .map(BattleSubmission::getRoomChallengeId)
                .distinct()
                .count();

        int totalAttempts = submissions.size();
        int currentChallengePosition = Math.min((int) challengesCompleted + 1, challengeCount);
        boolean isFinished = challengesCompleted >= challengeCount;

        String username = resolveUsername(participant.getUserId());
        String avatarUrl = userRepository.findByKeycloakId(participant.getUserId())
                .map(User::getAvatarUrl)
                .orElse(null);

        return ArenaParticipantProgressResponse.builder()
                .participantId(participantId)
                .userId(participant.getUserId())
                .username(username)
                .avatarUrl(avatarUrl)
                .challengesCompleted((int) challengesCompleted)
                .currentChallengePosition(currentChallengePosition)
                .totalAttempts(totalAttempts)
                .isFinished(isFinished)
                .build();
    }

    /**
     * Checks if a player has solved all challenges in the room.
     */
    private boolean isPlayerFinished(BattleParticipant participant, int challengeCount) {
        String participantId = participant.getId().toString();
        List<BattleSubmission> submissions = submissionRepository
                .findByParticipantIdOrderBySubmittedAtAsc(participantId);

        long acceptedCount = submissions.stream()
                .filter(s -> s.getStatus() == BattleSubmissionStatus.ACCEPTED)
                .map(BattleSubmission::getRoomChallengeId)
                .distinct()
                .count();

        return acceptedCount >= challengeCount;
    }

    /**
     * Calculates remaining seconds for the match.
     * Returns -1 for PRACTICE and DAILY modes (no time limit).
     */
    private long calculateRemainingSeconds(BattleRoom room) {
        BattleMode mode = room.getMode();
        if (mode == BattleMode.PRACTICE || mode == BattleMode.DAILY) {
            return -1;
        }

        int limitMinutes = getTimeLimitMinutes(mode);
        if (limitMinutes <= 0) {
            return -1;
        }

        if (room.getStartsAt() == null) {
            return -1;
        }

        LocalDateTime endTime = room.getStartsAt().plusMinutes(limitMinutes);
        long remaining = Duration.between(LocalDateTime.now(), endTime).getSeconds();
        return Math.max(remaining, 0);
    }

    private int getTimeLimitMinutes(BattleMode mode) {
        return switch (mode) {
            case DUEL -> timeLimitProperties.getDuelMinutes();
            case TEAM -> timeLimitProperties.getTeamMinutes();
            case RANKED_ARENA -> timeLimitProperties.getRankedArenaMinutes();
            case BLITZ -> timeLimitProperties.getBlitzMinutes();
            case PRACTICE -> timeLimitProperties.getPracticeMinutes();
            case DAILY -> timeLimitProperties.getDailyMinutes();
        };
    }

    private String resolveUsername(String userId) {
        return userRepository.findByKeycloakId(userId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getFirstName())
                .orElse(userId);
    }

    /**
     * Builds final standings for a room, ordered by challengesCompleted DESC, totalAttempts ASC.
     * Used by BattleRoomEventListener to build MatchFinishedEvent.
     */
    public List<ArenaParticipantProgressResponse> buildFinalStandings(String roomId) {
        BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId)).orElse(null);
        if (room == null) return List.of();

        List<BattleParticipant> players = participantRepository.findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);

        return players.stream()
                .map(p -> buildParticipantProgress(p, room.getChallengeCount()))
                .sorted(Comparator
                        .comparingInt(ArenaParticipantProgressResponse::getChallengesCompleted).reversed()
                        .thenComparingInt(ArenaParticipantProgressResponse::getTotalAttempts))
                .toList();
    }
}

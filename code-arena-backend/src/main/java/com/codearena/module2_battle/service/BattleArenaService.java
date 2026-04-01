package com.codearena.module2_battle.service;

import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.TestCase;
import com.codearena.module1_challenge.repository.ChallengeRepository;
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
import com.codearena.module2_battle.util.Judge0LanguageMapper;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
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
    private final UserRepository userRepository;
    private final Judge0Client judge0Client;
    private final Judge0LanguageMapper languageMapper;
    private final ArenaBroadcastService arenaBroadcastService;
    private final BattleRoomStateMachineService stateMachineService;
    private final com.codearena.module2_battle.config.TimeLimitProperties timeLimitProperties;
    private final Executor submissionExecutor;

    // Lock object for thread-safe match completion checks
    private final Object matchCompletionLock = new Object();

    public BattleArenaService(
            BattleRoomRepository battleRoomRepository,
            BattleParticipantRepository participantRepository,
            BattleRoomChallengeRepository roomChallengeRepository,
            BattleSubmissionRepository submissionRepository,
            ChallengeRepository challengeRepository,
            UserRepository userRepository,
            Judge0Client judge0Client,
            Judge0LanguageMapper languageMapper,
            ArenaBroadcastService arenaBroadcastService,
            BattleRoomStateMachineService stateMachineService,
            com.codearena.module2_battle.config.TimeLimitProperties timeLimitProperties,
            @Qualifier("submissionExecutor") Executor submissionExecutor) {
        this.battleRoomRepository = battleRoomRepository;
        this.participantRepository = participantRepository;
        this.roomChallengeRepository = roomChallengeRepository;
        this.submissionRepository = submissionRepository;
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.judge0Client = judge0Client;
        this.languageMapper = languageMapper;
        this.arenaBroadcastService = arenaBroadcastService;
        this.stateMachineService = stateMachineService;
        this.timeLimitProperties = timeLimitProperties;
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
     * and returned to the caller. Judge0 execution and result processing happen asynchronously
     * on the submissionExecutor thread pool — the HTTP response is never blocked by Judge0.
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

        // Persist submission immediately with PENDING status before calling Judge0
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

        // Load all test cases (including hidden ones) for judging
        Challenge challenge = challengeRepository.findById(Long.parseLong(roomChallenge.getChallengeId()))
                .orElseThrow(() -> new BattleRoomNotFoundException(roomChallenge.getChallengeId()));
        List<TestCase> allTestCases = challenge.getTestCases();
        int totalTestCases = allTestCases.size();

        // Map language to Judge0 language ID
        int languageId = languageMapper.toJudge0Id(request.getLanguage());

        // Build Judge0 request — concatenate all test inputs/outputs
        String stdin = allTestCases.stream().map(TestCase::getInput).collect(Collectors.joining("\n"));
        String expectedOutput = allTestCases.stream().map(TestCase::getExpectedOutput).collect(Collectors.joining("\n"));

        Judge0SubmissionRequest judge0Request = Judge0SubmissionRequest.builder()
                .languageId(languageId)
                .sourceCode(request.getCode())
                .stdin(stdin)
                .expectedOutput(expectedOutput)
                .build();

        // Submit to Judge0 and process result asynchronously on the submissionExecutor thread pool.
        // This ensures the HTTP response returns immediately with PENDING status.
        submissionExecutor.execute(() -> processJudge0Submission(
                judge0Request, submissionId, roomId, participantId,
                request.getRoomChallengeId(), challengePosition, userId,
                room.getChallengeCount(), attemptNumber, totalTestCases
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

    /**
     * Async callback: submits code to Judge0, polls for result, updates the submission record,
     * and broadcasts results to arena subscribers.
     */
    private void processJudge0Submission(
            Judge0SubmissionRequest judge0Request, String submissionId, String roomId,
            String participantId, String roomChallengeId, int challengePosition,
            String userId, int challengeCount, int attemptNumber, int totalTestCases) {
        try {
            String token = judge0Client.submitCode(judge0Request);

            // Poll Judge0 at 1-second intervals until result is ready or timeout
            Judge0SubmissionResult result = pollJudge0Result(token);

            // Map Judge0 result to our submission status
            BattleSubmissionStatus status = mapJudge0Status(result);
            Integer runtimeMs = result.getTime() != null ? (int) (result.getTime() * 1000) : null;
            Integer memoryKb = result.getMemory();
            String feedback = buildFeedback(status, runtimeMs, result.getCompileOutput(), totalTestCases);

            // Update the submission record
            BattleSubmission submission = submissionRepository.findById(UUID.fromString(submissionId)).orElse(null);
            if (submission != null) {
                submission.setStatus(status);
                submission.setRuntimeMs(runtimeMs);
                submission.setMemoryKb(memoryKb);
                submissionRepository.save(submission);
            }

            boolean isAccepted = status == BattleSubmissionStatus.ACCEPTED;

            // Send result to the submitting player only
            SubmissionResultResponse resultResponse = SubmissionResultResponse.builder()
                    .submissionId(submissionId)
                    .roomChallengeId(roomChallengeId)
                    .status(status.name())
                    .attemptNumber(attemptNumber)
                    .runtimeMs(runtimeMs)
                    .memoryKb(memoryKb)
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
                        .submissionStatus(status)
                        .attemptNumber(attemptNumber)
                        .delayedAtTimestamp(System.currentTimeMillis())
                        .build();
                arenaBroadcastService.broadcastSpectatorFeedDelayed(roomId, spectatorEvent);

                // If accepted, check if this player has solved all challenges
                if (isAccepted && progress.getChallengesCompleted() >= challengeCount) {
                    checkMatchCompletion(roomId);
                }
            }

        } catch (Judge0UnavailableException e) {
            log.error("Judge0 unavailable for submission {}: {}", submissionId, e.getMessage());
            // Mark submission as COMPILE_ERROR and notify the player
            BattleSubmission submission = submissionRepository.findById(UUID.fromString(submissionId)).orElse(null);
            if (submission != null) {
                submission.setStatus(BattleSubmissionStatus.COMPILE_ERROR);
                submissionRepository.save(submission);
            }

            SubmissionResultResponse errorResponse = SubmissionResultResponse.builder()
                    .submissionId(submissionId)
                    .roomChallengeId(roomChallengeId)
                    .status(BattleSubmissionStatus.COMPILE_ERROR.name())
                    .attemptNumber(attemptNumber)
                    .feedback("Code execution service is temporarily unavailable — please retry")
                    .isAccepted(false)
                    .build();
            arenaBroadcastService.sendSubmissionResult(userId, errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error processing submission {}: {}", submissionId, e.getMessage(), e);
        }
    }

    /**
     * Polls Judge0 at 1-second intervals until the submission result is final (statusId >= 3)
     * or the configured timeout is reached.
     */
    private Judge0SubmissionResult pollJudge0Result(String token) {
        int timeoutSeconds = judge0Client.getTimeoutSeconds();
        int elapsed = 0;

        while (elapsed < timeoutSeconds) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            elapsed++;

            Judge0SubmissionResult result = judge0Client.getResult(token);
            if (result != null && result.getStatus() != null && result.getStatus().getId() >= 3) {
                return result;
            }
        }

        // Timeout — treat as TIME_LIMIT
        Judge0SubmissionResult timeoutResult = new Judge0SubmissionResult();
        Judge0SubmissionResult.Judge0Status timeoutStatus = new Judge0SubmissionResult.Judge0Status();
        timeoutStatus.setId(5); // Time Limit Exceeded
        timeoutStatus.setDescription("Time Limit Exceeded (polling timeout)");
        timeoutResult.setStatus(timeoutStatus);
        timeoutResult.setTime((double) timeoutSeconds);
        timeoutResult.setToken(token);
        return timeoutResult;
    }

    /**
     * Maps Judge0 status ID to our BattleSubmissionStatus.
     */
    private BattleSubmissionStatus mapJudge0Status(Judge0SubmissionResult result) {
        if (result == null || result.getStatus() == null) {
            return BattleSubmissionStatus.RUNTIME_ERROR;
        }
        int statusId = result.getStatus().getId();
        return switch (statusId) {
            case 3 -> BattleSubmissionStatus.ACCEPTED;
            case 4 -> BattleSubmissionStatus.WRONG_ANSWER;
            case 5 -> BattleSubmissionStatus.TIME_LIMIT;
            case 6 -> BattleSubmissionStatus.COMPILE_ERROR;
            default -> {
                if (statusId >= 7 && statusId <= 12) {
                    yield BattleSubmissionStatus.RUNTIME_ERROR;
                }
                yield BattleSubmissionStatus.PENDING;
            }
        };
    }

    /**
     * Builds a user-facing feedback string based on submission status.
     * Does not reveal which specific test case failed to prevent gaming.
     */
    private String buildFeedback(BattleSubmissionStatus status, Integer runtimeMs,
                                 String compileOutput, int totalTestCases) {
        return switch (status) {
            case ACCEPTED -> "All " + totalTestCases + " test cases passed";
            case WRONG_ANSWER -> "Wrong answer — check your logic";
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
     * Hidden test cases are used by Judge0 for judging but never appear in any response DTO.
     */
    private ArenaChallengeResponse buildArenaChallengeResponse(BattleRoomChallenge rc) {
        Challenge challenge = challengeRepository.findById(Long.parseLong(rc.getChallengeId())).orElse(null);
        if (challenge == null) {
            return ArenaChallengeResponse.builder()
                    .roomChallengeId(rc.getId().toString())
                    .position(rc.getPosition())
                    .challengeId(rc.getChallengeId())
                    .build();
        }

        // Only expose non-hidden test cases
        List<VisibleTestCaseResponse> visibleTests = challenge.getTestCases().stream()
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

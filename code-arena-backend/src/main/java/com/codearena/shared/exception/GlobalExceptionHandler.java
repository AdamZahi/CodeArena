package com.codearena.shared.exception;

import com.codearena.module2_battle.exception.*;
import com.codearena.shared.response.ApiResponse;
import com.codearena.shared.response.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles custom API exceptions.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    /**
     * Handles bean validation exceptions.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<Void>builder()
            .success(false).message("Validation failed").timestamp(Instant.now()).build());
    }

    /**
     * Handles authorization exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.<Void>builder()
            .success(false).message("Access denied").timestamp(Instant.now()).build());
    }

    // ── Battle module exceptions ──

    @ExceptionHandler(BattleRoomNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBattleRoomNotFound(BattleRoomNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(InvalidRoomConfigException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRoomConfig(InvalidRoomConfigException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(RoomNotJoinableException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoomNotJoinable(RoomNotJoinableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(PlayersNotReadyException.class)
    public ResponseEntity<ApiResponse<Void>> handlePlayersNotReady(PlayersNotReadyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(UnauthorizedRoomActionException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedRoomAction(UnauthorizedRoomActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(ParticipantNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleParticipantNotFound(ParticipantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(InvalidParticipantActionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidParticipantAction(InvalidParticipantActionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(NotEnoughChallengesException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotEnoughChallenges(NotEnoughChallengesException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(BattleRoomFullException.class)
    public ResponseEntity<ApiResponse<Void>> handleBattleRoomFull(BattleRoomFullException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(DuplicateParticipantException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateParticipant(DuplicateParticipantException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(InvalidRoomTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRoomTransition(InvalidRoomTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    // ── Arena-phase exceptions (Step 3) ──

    @ExceptionHandler(ArenaNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleArenaNotActive(ArenaNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(ChallengeAlreadySolvedException.class)
    public ResponseEntity<ApiResponse<Void>> handleChallengeAlreadySolved(ChallengeAlreadySolvedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(UnsupportedLanguageException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedLanguage(UnsupportedLanguageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSubmissionNotFound(SubmissionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(Judge0UnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleJudge0Unavailable(Judge0UnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    // ── Post-match exceptions (Step 4) ──

    @ExceptionHandler(ResultsNotReadyException.class)
    public ResponseEntity<ApiResponse<Void>> handleResultsNotReady(ResultsNotReadyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(ActiveSeasonNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleActiveSeasonNotFound(ActiveSeasonNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    // ── Step 5 exceptions ──

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(SeasonAlreadyActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleSeasonAlreadyActive(SeasonAlreadyActiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    @ExceptionHandler(DailyChallengeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleDailyChallengeNotFound(DailyChallengeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }
}

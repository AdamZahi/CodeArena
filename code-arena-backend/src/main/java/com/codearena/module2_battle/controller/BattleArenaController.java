package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.ArenaChallengeResponse;
import com.codearena.module2_battle.dto.ArenaStateResponse;
import com.codearena.module2_battle.dto.SubmissionResultResponse;
import com.codearena.module2_battle.dto.SubmitSolutionRequest;
import com.codearena.module2_battle.dto.ActivityRequest;
import com.codearena.module2_battle.service.BattleArenaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/battle/arena")
@RequiredArgsConstructor
public class BattleArenaController {

    private final BattleArenaService battleArenaService;

    @GetMapping("/{roomId}/state")
    public ResponseEntity<ArenaStateResponse> getArenaState(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        ArenaStateResponse state = battleArenaService.getArenaState(roomId, userId);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/{roomId}/submit")
    public ResponseEntity<SubmissionResultResponse> submitSolution(
            @PathVariable String roomId,
            @RequestBody @Valid SubmitSolutionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        request.setRoomId(roomId);
        SubmissionResultResponse result = battleArenaService.submitSolution(userId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{roomId}/participants/{participantId}/submissions")
    public ResponseEntity<List<SubmissionResultResponse>> getParticipantSubmissions(
            @PathVariable String roomId,
            @PathVariable String participantId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<SubmissionResultResponse> submissions =
                battleArenaService.getSubmissionsForParticipant(roomId, participantId, userId);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/{roomId}/challenges")
    public ResponseEntity<List<ArenaChallengeResponse>> getRoomChallenges(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<ArenaChallengeResponse> challenges = battleArenaService.getRoomChallenges(roomId, userId);
        return ResponseEntity.ok(challenges);
    }

    @PostMapping("/{roomId}/activity")
    public ResponseEntity<Void> reportActivity(
            @PathVariable String roomId,
            @RequestBody ActivityRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        battleArenaService.reportActivity(roomId, userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/reconnect")
    public ResponseEntity<ArenaStateResponse> reconnect(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        ArenaStateResponse state = battleArenaService.handleReconnect(roomId, userId);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/{roomId}/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        battleArenaService.handleHeartbeat(roomId, userId);
        return ResponseEntity.ok().build();
    }
}

package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.service.BattleArenaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
            JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        ArenaStateResponse state = battleArenaService.getArenaState(roomId, userId);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/{roomId}/submit")
    public ResponseEntity<SubmissionResultResponse> submitSolution(
            @PathVariable String roomId,
            @RequestBody @Valid SubmitSolutionRequest request,
            JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        request.setRoomId(roomId);
        SubmissionResultResponse result = battleArenaService.submitSolution(userId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{roomId}/participants/{participantId}/submissions")
    public ResponseEntity<List<SubmissionResultResponse>> getParticipantSubmissions(
            @PathVariable String roomId,
            @PathVariable String participantId,
            JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        List<SubmissionResultResponse> submissions =
                battleArenaService.getSubmissionsForParticipant(roomId, participantId, userId);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/{roomId}/challenges")
    public ResponseEntity<List<ArenaChallengeResponse>> getRoomChallenges(
            @PathVariable String roomId,
            JwtAuthenticationToken principal) {
        String userId = principal.getToken().getSubject();
        List<ArenaChallengeResponse> challenges = battleArenaService.getRoomChallenges(roomId, userId);
        return ResponseEntity.ok(challenges);
    }
}

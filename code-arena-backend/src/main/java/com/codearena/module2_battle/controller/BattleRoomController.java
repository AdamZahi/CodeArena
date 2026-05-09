package com.codearena.module2_battle.controller;

import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.service.BattleRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/battle/rooms")
@RequiredArgsConstructor
public class BattleRoomController {

    private final BattleRoomService battleRoomService;

    @PostMapping
    public ResponseEntity<RoomCreatedResponse> createRoom(
            @RequestBody @Valid CreateRoomRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(battleRoomService.createRoom(userId, request));
    }

    @GetMapping("/public")
    public ResponseEntity<List<BattleRoomResponse>> getPublicRooms() {
        return ResponseEntity.ok(battleRoomService.getPublicRooms());
    }

    @GetMapping("/{roomId}/lobby")
    public ResponseEntity<LobbyStateResponse> getLobbyState(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(battleRoomService.getLobbyState(roomId, userId));
    }

    @PostMapping("/join")
    public ResponseEntity<LobbyStateResponse> joinRoom(
            @RequestBody @Valid JoinRoomRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(battleRoomService.joinRoom(userId, request));
    }

    @PostMapping("/{roomId}/ready")
    public ResponseEntity<LobbyStateResponse> toggleReady(
            @PathVariable String roomId,
            @RequestBody @Valid ReadyToggleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(battleRoomService.toggleReady(roomId, userId, request));
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<LobbyStateResponse> startBattle(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(battleRoomService.startBattle(roomId, userId));
    }

    @PostMapping("/{roomId}/kick")
    public ResponseEntity<LobbyStateResponse> kickParticipant(
            @PathVariable String roomId,
            @RequestBody @Valid KickParticipantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(battleRoomService.kickParticipant(roomId, userId, request));
    }

    @DeleteMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        battleRoomService.leaveRoom(roomId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/invite")
    public ResponseEntity<InviteLinkResponse> getInviteLink(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(battleRoomService.getInviteLink(roomId, userId));
    }
}

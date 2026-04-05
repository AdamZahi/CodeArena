package com.codearena.module2_battle.service;

import com.codearena.module2_battle.dto.PlayerDisconnectedEvent;
import com.codearena.module2_battle.dto.PlayerReconnectedEvent;
import com.codearena.module2_battle.entity.BattleParticipant;
import com.codearena.module2_battle.entity.BattleRoom;
import com.codearena.module2_battle.enums.BattleRoomStatus;
import com.codearena.module2_battle.enums.ParticipantRole;
import com.codearena.module2_battle.repository.BattleParticipantRepository;
import com.codearena.module2_battle.repository.BattleRoomRepository;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Tracks WebSocket sessions to battle rooms and manages disconnect/reconnect lifecycle.
 * Thread-safe: all maps are ConcurrentHashMap.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleConnectionTracker {

    private final BattleParticipantRepository participantRepository;
    private final BattleRoomRepository battleRoomRepository;
    private final UserRepository userRepository;
    private final ArenaBroadcastService arenaBroadcastService;
    private final BattleRoomStateMachineService stateMachineService;

    private final ScheduledExecutorService forfeitScheduler = Executors.newScheduledThreadPool(2);

    private static final int RECONNECT_GRACE_SECONDS = 30;

    /** sessionId → { roomId, userId } */
    private final ConcurrentHashMap<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();

    /** participantId → scheduled forfeit task handle */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> forfeitTasks = new ConcurrentHashMap<>();

    /** userId → last heartbeat timestamp */
    private final ConcurrentHashMap<String, Instant> heartbeats = new ConcurrentHashMap<>();

    /** userId → connected state (for rooms that are IN_PROGRESS) */
    private final ConcurrentHashMap<String, Boolean> connectionState = new ConcurrentHashMap<>();

    public record SessionInfo(String roomId, String userId) {}

    public void registerSession(String sessionId, String roomId, String userId) {
        sessionMap.put(sessionId, new SessionInfo(roomId, userId));
        connectionState.put(userId, true);
        heartbeats.put(userId, Instant.now());
        log.debug("Registered WS session {} for user {} in room {}", sessionId, userId, roomId);
    }

    public void updateHeartbeat(String userId) {
        heartbeats.put(userId, Instant.now());
    }

    public boolean isConnected(String userId) {
        return Boolean.TRUE.equals(connectionState.get(userId));
    }

    /**
     * Called when a WebSocket session disconnects.
     */
    public void onSessionDisconnect(String sessionId) {
        SessionInfo info = sessionMap.remove(sessionId);
        if (info == null) return;

        String roomId = info.roomId();
        String userId = info.userId();
        connectionState.put(userId, false);

        // Only handle disconnect for IN_PROGRESS rooms and PLAYER participants
        BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId)).orElse(null);
        if (room == null || room.getStatus() != BattleRoomStatus.IN_PROGRESS) return;

        BattleParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, userId).orElse(null);
        if (participant == null || participant.getRole() != ParticipantRole.PLAYER) return;

        String participantId = participant.getId().toString();
        String displayName = resolveUsername(userId);

        log.info("Player {} disconnected from room {} — starting {}s forfeit timer",
                displayName, roomId, RECONNECT_GRACE_SECONDS);

        // Broadcast PLAYER_DISCONNECTED
        arenaBroadcastService.broadcastPlayerDisconnected(roomId, PlayerDisconnectedEvent.builder()
                .participantId(participantId)
                .displayName(displayName)
                .reconnectDeadlineSeconds(RECONNECT_GRACE_SECONDS)
                .build());

        // Schedule forfeit task
        ScheduledFuture<?> task = forfeitScheduler.schedule(
                () -> executeForfeit(roomId, userId, participantId),
                RECONNECT_GRACE_SECONDS, TimeUnit.SECONDS);
        forfeitTasks.put(participantId, task);
    }

    /**
     * Called when a player reconnects within the grace period.
     * Returns true if reconnect was successful (forfeit cancelled).
     */
    public boolean onReconnect(String roomId, String userId) {
        connectionState.put(userId, true);
        heartbeats.put(userId, Instant.now());

        BattleParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, userId).orElse(null);
        if (participant == null) return false;

        String participantId = participant.getId().toString();

        // Cancel forfeit task
        ScheduledFuture<?> task = forfeitTasks.remove(participantId);
        if (task != null) {
            task.cancel(false);
            log.info("Player {} reconnected to room {} — forfeit cancelled", userId, roomId);
        }

        String displayName = resolveUsername(userId);
        arenaBroadcastService.broadcastPlayerReconnected(roomId, PlayerReconnectedEvent.builder()
                .participantId(participantId)
                .displayName(displayName)
                .build());

        return true;
    }

    /**
     * Executes forfeit after the grace period expires.
     */
    private void executeForfeit(String roomId, String userId, String participantId) {
        forfeitTasks.remove(participantId);

        // Re-check if player reconnected (race condition guard)
        if (Boolean.TRUE.equals(connectionState.get(userId))) {
            log.debug("Player {} reconnected before forfeit fired — skipping", userId);
            return;
        }

        BattleRoom room = battleRoomRepository.findById(UUID.fromString(roomId)).orElse(null);
        if (room == null || room.getStatus() != BattleRoomStatus.IN_PROGRESS) return;

        // Check if any players are still connected
        List<BattleParticipant> players = participantRepository.findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);
        boolean anyConnected = players.stream()
                .anyMatch(p -> Boolean.TRUE.equals(connectionState.get(p.getUserId())));

        try {
            if (anyConnected) {
                log.info("Forfeit: player {} timed out in room {} — transitioning to FINISHED", userId, roomId);
                stateMachineService.transitionToFinished(roomId);
            } else {
                log.info("Forfeit: all players disconnected in room {} — transitioning to CANCELLED", userId, roomId);
                stateMachineService.transitionToCancelled(roomId, "All players disconnected");
            }
        } catch (Exception e) {
            log.error("Forfeit transition failed for room {}: {}", roomId, e.getMessage(), e);
        }
    }

    /**
     * Cleanup: cancel all forfeit tasks for a room (called when room finishes normally).
     */
    public void cleanupRoom(String roomId) {
        List<BattleParticipant> players = participantRepository.findByRoomIdAndRole(roomId, ParticipantRole.PLAYER);
        for (BattleParticipant p : players) {
            ScheduledFuture<?> task = forfeitTasks.remove(p.getId().toString());
            if (task != null) task.cancel(false);
            connectionState.remove(p.getUserId());
        }
    }

    private String resolveUsername(String userId) {
        return userRepository.findByKeycloakId(userId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getFirstName())
                .orElse(userId);
    }
}

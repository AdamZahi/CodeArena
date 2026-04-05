package com.codearena.module2_battle.service;

import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.enums.LobbyEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Single point of contact for all arena-phase WebSocket broadcasts.
 * Mirrors the structure of LobbyBroadcastService for the live battle phase.
 * No direct SimpMessagingTemplate calls should exist in arena services or controllers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArenaBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;

    private static final String ARENA_TOPIC_PREFIX = "/topic/battle/arena/";
    private static final String SPECTATOR_TOPIC_PREFIX = "/topic/battle/spectator/";
    private static final long SPECTATOR_DELAY_SECONDS = 30;

    /**
     * Sends full arena state to all subscribers of /topic/battle/arena/{roomId}.
     */
    public void broadcastArenaState(String roomId, ArenaStateResponse state) {
        LobbyEvent event = LobbyEvent.builder()
                .type(LobbyEventType.ARENA_STATE)
                .roomId(roomId)
                .payload(state)
                .build();
        messagingTemplate.convertAndSend(ARENA_TOPIC_PREFIX + roomId, event);
    }

    /**
     * Sends SUBMISSION_RESULT to a specific user session only.
     * Uses STOMP user destinations (/user/queue/battle/submission) so only the
     * submitting player receives their own result.
     */
    public void sendSubmissionResult(String username, SubmissionResultResponse result) {
        LobbyEvent event = LobbyEvent.builder()
                .type(LobbyEventType.SUBMISSION_RESULT)
                .roomId(null)
                .payload(result)
                .build();
        messagingTemplate.convertAndSendToUser(username, "/queue/battle/submission", event);
    }

    /**
     * Sends OPPONENT_PROGRESS to all arena subscribers.
     * The client-side filters out progress events for the current user.
     */
    public void broadcastOpponentProgress(String roomId, String excludeUserId, OpponentProgressEvent progressEvent) {
        LobbyEvent event = LobbyEvent.builder()
                .type(LobbyEventType.OPPONENT_PROGRESS)
                .roomId(roomId)
                .payload(progressEvent)
                .build();
        messagingTemplate.convertAndSend(ARENA_TOPIC_PREFIX + roomId, event);
        log.debug("Broadcast OPPONENT_PROGRESS for room {} (exclude userId: {})", roomId, excludeUserId);
    }

    /**
     * Sends SPECTATOR_FEED to /topic/battle/spectator/{roomId} after a 30-second delay.
     * Uses TaskScheduler to delay delivery, matching the spectator delay requirement.
     */
    public void broadcastSpectatorFeedDelayed(String roomId, SpectatorFeedEvent feedEvent) {
        taskScheduler.schedule(() -> {
            LobbyEvent event = LobbyEvent.builder()
                    .type(LobbyEventType.SPECTATOR_FEED)
                    .roomId(roomId)
                    .payload(feedEvent)
                    .build();
            messagingTemplate.convertAndSend(SPECTATOR_TOPIC_PREFIX + roomId, event);
            log.debug("Delivered delayed SPECTATOR_FEED for room {}", roomId);
        }, Instant.now().plusSeconds(SPECTATOR_DELAY_SECONDS));
    }

    /**
     * Sends MATCH_FINISHED to all arena subscribers.
     */
    public void broadcastMatchFinished(String roomId, MatchFinishedEvent finishedEvent) {
        LobbyEvent event = LobbyEvent.builder()
                .type(LobbyEventType.MATCH_FINISHED)
                .roomId(roomId)
                .payload(finishedEvent)
                .build();
        messagingTemplate.convertAndSend(ARENA_TOPIC_PREFIX + roomId, event);
    }

    /**
     * Sends MATCH_CANCELLED to all arena subscribers.
     */
    public void broadcastMatchCancelled(String roomId, String reason) {
        LobbyEvent event = LobbyEvent.builder()
                .type(LobbyEventType.MATCH_CANCELLED)
                .roomId(roomId)
                .payload(reason)
                .build();
        messagingTemplate.convertAndSend(ARENA_TOPIC_PREFIX + roomId, event);
    }

    /**
     * Sends OPPONENT_ACTIVITY to all arena subscribers (client filters out self).
     */
    public void broadcastActivity(String roomId, OpponentActivityEvent activityEvent) {
        LobbyEvent event = LobbyEvent.builder()
                .type(LobbyEventType.OPPONENT_ACTIVITY)
                .roomId(roomId)
                .payload(activityEvent)
                .build();
        messagingTemplate.convertAndSend(ARENA_TOPIC_PREFIX + roomId, event);
    }

    /**
     * Sends TEST_CASE_PROGRESS to a specific user's private queue.
     */
    public void sendTestCaseProgress(String username, TestCaseProgressEvent progress) {
        LobbyEvent event = LobbyEvent.builder()
                .type(LobbyEventType.TEST_CASE_PROGRESS)
                .roomId(null)
                .payload(progress)
                .build();
        messagingTemplate.convertAndSendToUser(username, "/queue/battle/submission", event);
    }

    /**
     * Sends PLAYER_DISCONNECTED to all arena subscribers.
     */
    public void broadcastPlayerDisconnected(String roomId, Object payload) {
        LobbyEvent event = LobbyEvent.builder()
                .type(LobbyEventType.PLAYER_DISCONNECTED)
                .roomId(roomId)
                .payload(payload)
                .build();
        messagingTemplate.convertAndSend(ARENA_TOPIC_PREFIX + roomId, event);
    }

    /**
     * Sends PLAYER_RECONNECTED to all arena subscribers.
     */
    public void broadcastPlayerReconnected(String roomId, Object payload) {
        LobbyEvent event = LobbyEvent.builder()
                .type(LobbyEventType.PLAYER_RECONNECTED)
                .roomId(roomId)
                .payload(payload)
                .build();
        messagingTemplate.convertAndSend(ARENA_TOPIC_PREFIX + roomId, event);
    }
}

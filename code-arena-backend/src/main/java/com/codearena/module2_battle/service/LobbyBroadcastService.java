package com.codearena.module2_battle.service;

import com.codearena.module2_battle.dto.*;
import com.codearena.module2_battle.enums.LobbyEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Single point of contact for all WebSocket broadcasts to the battle lobby.
 * No direct SimpMessagingTemplate calls should exist outside this service.
 */
@Service
@RequiredArgsConstructor
public class LobbyBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_PREFIX = "/topic/battle/lobby/";

    public void broadcastLobbyState(String roomId, LobbyStateResponse state) {
        send(roomId, LobbyEventType.LOBBY_STATE, state);
    }

    public void broadcastPlayerJoined(String roomId, ParticipantResponse participant) {
        send(roomId, LobbyEventType.PLAYER_JOINED, participant);
    }

    public void broadcastPlayerLeft(String roomId, String userId) {
        send(roomId, LobbyEventType.PLAYER_LEFT, userId);
    }

    public void broadcastPlayerKicked(String roomId, String userId) {
        send(roomId, LobbyEventType.PLAYER_KICKED, userId);
    }

    public void broadcastReadyChanged(String roomId, ParticipantResponse participant) {
        send(roomId, LobbyEventType.READY_CHANGED, participant);
    }

    public void broadcastCountdownStarted(String roomId, CountdownPayload payload) {
        send(roomId, LobbyEventType.COUNTDOWN_STARTED, payload);
    }

    public void broadcastBattleStarted(String roomId, BattleRoomResponse room) {
        send(roomId, LobbyEventType.BATTLE_STARTED, room);
    }

    public void broadcastRoomCancelled(String roomId, String reason) {
        send(roomId, LobbyEventType.ROOM_CANCELLED, reason);
    }

    private void send(String roomId, String type, Object payload) {
        LobbyEvent event = LobbyEvent.builder()
                .type(type)
                .roomId(roomId)
                .payload(payload)
                .build();
        messagingTemplate.convertAndSend(TOPIC_PREFIX + roomId, event);
    }
}

package com.codearena.module2_battle.event;

import com.codearena.module2_battle.service.BattleConnectionTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

/**
 * Listens for WebSocket session lifecycle events to track player connections.
 * On subscribe to an arena topic: registers the session.
 * On disconnect: triggers the forfeit grace period.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BattleWebSocketEventListener {

    private final BattleConnectionTracker connectionTracker;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/topic/battle/arena/")) return;

        // Extract roomId from /topic/battle/arena/{roomId}
        String roomId = destination.substring("/topic/battle/arena/".length());
        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();
        if (user == null || sessionId == null) return;

        String userId = user.getName();
        connectionTracker.registerSession(sessionId, roomId, userId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) return;

        connectionTracker.onSessionDisconnect(sessionId);
    }
}

package com.codearena.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * Configures broker prefixes.
     * /topic → broadcast to all subscribers
     * /queue → point-to-point messages
     * /app  → messages routed to @MessageMapping methods
     * /user → user-specific destinations
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("wss-heartbeat-thread-");
        taskScheduler.initialize();

        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(taskScheduler);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Registers TWO websocket endpoints:
     * 1. /ws          → SockJS endpoint (used by battle module)
     * 2. /ws/websocket → Raw WebSocket endpoint (used by shop notification service)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ── SOCKJS ENDPOINT ───────────────────────────────────────────────
        // Used by battle module: battle-websocket.service.ts
        // SockJS provides fallback transports for older browsers
        // URL: http://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // ── RAW WEBSOCKET ENDPOINT ────────────────────────────────────────
        // Used by shop notification service: notification.service.ts
        // Direct WebSocket — no SockJS overhead, no CORS credential issues
        // URL: ws://localhost:8080/ws/websocket
        // JWT auth handled by WebSocketAuthInterceptor via STOMP connectHeaders
        registry.addEndpoint("/ws/websocket")
                .setAllowedOriginPatterns("*");
    }

    /**
     * Registers the JWT auth interceptor on the inbound channel so that
     * STOMP CONNECT frames carry a Principal for user-destination routing.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
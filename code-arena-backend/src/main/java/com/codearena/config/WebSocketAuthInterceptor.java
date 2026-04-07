package com.codearena.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Intercepts STOMP CONNECT frames, extracts the JWT from the Authorization header,
 * validates it, and sets the Principal on the session. This allows
 * SimpMessagingTemplate.convertAndSendToUser() to route messages to the correct session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthConverter jwtAuthConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    JwtAuthenticationToken authentication =
                            (JwtAuthenticationToken) jwtAuthConverter.convert(jwt);
                    accessor.setUser(authentication);
                    log.debug("[WebSocketAuth] Authenticated user: {}", jwt.getSubject());
                } catch (Exception e) {
                    log.warn("[WebSocketAuth] JWT validation failed: {}", e.getMessage());
                }
            }
        }

        return message;
    }
}

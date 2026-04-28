package com.codearena.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures broker prefixes.cd CodeArena
     * git checkout final-merge
     * npm install
     * npm run dev
     *
     * @param registry message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // TODO: Tune broker channels for battle throughput.
        registry.enableSimpleBroker("/topic", "/queue"); // added /queue for voice signaling
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user"); // added for user-specific voice messages
    }

    /**
     * Registers websocket endpoints.
     *
     * @param registry stomp endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // TODO: Enable proper allowed origins for deployment.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
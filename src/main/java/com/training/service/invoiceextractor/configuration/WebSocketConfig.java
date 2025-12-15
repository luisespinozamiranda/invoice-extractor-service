package com.training.service.invoiceextractor.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time extraction progress updates.
 *
 * <p>Enables STOMP over WebSocket for bidirectional communication between
 * server and clients. Clients can subscribe to topics to receive real-time
 * updates about extraction progress.
 *
 * <p><b>Architecture:</b> Configuration Layer (Inbound Adapter)
 * <p><b>Protocol:</b> STOMP (Simple Text Oriented Messaging Protocol)
 * <p><b>Fallback:</b> SockJS for browsers without WebSocket support
 *
 * @author Luis Espinoza
 * @version 1.0
 * @since 2025-12-12
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory message broker for broadcasting messages
        // Clients subscribe to topics prefixed with /topic
        config.enableSimpleBroker("/topic");

        // Application destination prefix for messages FROM clients TO server
        // Not used in this implementation (server pushes only)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint that clients connect to
        // Endpoint: ws://localhost:8080/ws-extraction
        registry.addEndpoint("/ws-extraction")
                .setAllowedOrigins(
                        "http://localhost",                   // Docker Nginx frontend (port 80)
                        "http://localhost:4200",
                        "https://*.vercel.app",
                        "https://*.netlify.app"
                )
                .withSockJS();  // Enable SockJS fallback for older browsers
    }
}

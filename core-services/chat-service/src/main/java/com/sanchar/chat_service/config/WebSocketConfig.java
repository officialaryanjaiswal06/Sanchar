package com.sanchar.chat_service.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{

    @Value("${chat.relay.host}")
    private String relayHost;

    @Value("${chat.relay.port}")
    private int relayPort;

    @Value("${chat.relay.client-login}")
    private String clientLogin;

    @Value("${chat.relay.client-passcode}")
    private String clientPasscode;

    @Value("${chat.relay.system-login}")
    private String systemLogin;

    @Value("${chat.relay.system-passcode}")
    private String systemPasscode;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The Endpoint the React Frontend connects to
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // Allows Frontend (Port 3000) connection
                .withSockJS(); // Enable fallback options
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Messages FROM Client destined for @MessageMapping start with /app
        registry.setApplicationDestinationPrefixes("/app");

        // EXTERNAL RABBITMQ RELAY (The Critical Link)
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(clientLogin)
                .setClientPasscode(clientPasscode)
                .setSystemLogin(systemLogin)
                .setSystemPasscode(systemPasscode);
    }
}

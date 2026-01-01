package com.sanchar.chat_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RedisTemplate<String, Object> redisTemplate; // Use generic Object or String template

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        // In the full version, we extract the User Principle from JWT
        // For MVP testing, the Frontend sends a header "login: userId" via Stomp
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // This relies on STOMP CONNECT header (Requires Frontend to send 'login')
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        // OR fallback to manually getting native headers if Principal not set
        // (Detailed auth logic comes in Priority 5, keeping simple for now)

        if (userId != null) {
            log.info("Received a new web socket connection: {}", userId);
            // Save to Redis: "online:users" set or individual key
            redisTemplate.opsForValue().set("status:online:" + userId, "true", 30, TimeUnit.MINUTES);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        if (userId != null) {
            log.info("User Disconnected: {}", userId);
            redisTemplate.delete("status:online:" + userId);
        }
    }
}

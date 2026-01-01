package com.sanchar.chat_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final StringRedisTemplate redisTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 1. Get User ID from Header (Sent by Frontend)
        String userId = headerAccessor.getFirstNativeHeader("userId");
        if (userId == null) {
            userId = headerAccessor.getFirstNativeHeader("userid"); // Safety check
        }

        if (userId != null) {
            log.info("🟢 User Connected: {}", userId);

            // 2. STORE ID IN SESSION (Critical for Disconnect Logic)
            // This allows us to retrieve "Who is this?" later when the socket closes.
            Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put("userId", userId);
            }

            // 3. Mark Online in Redis (30 Mins TTL)
            String key = "status:online:" + userId;
            redisTemplate.opsForValue().set(key, "true", 30, TimeUnit.MINUTES);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 4. RETRIEVE ID FROM SESSION (We saved this during Connect)
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        String userId = null;
        if (sessionAttributes != null) {
            userId = (String) sessionAttributes.get("userId");
        }

        if (userId != null) {
            log.info("🔴 User Disconnected: {}", userId);

            // 5. DELETE KEY (Instant Offline Status)
            String key = "status:online:" + userId;
            redisTemplate.delete(key);
        }
    }
}
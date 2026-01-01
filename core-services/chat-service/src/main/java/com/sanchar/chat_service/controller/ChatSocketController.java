package com.sanchar.chat_service.controller;

import com.sanchar.chat_service.config.RabbitConfig;
import com.sanchar.chat_service.model.ChatMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatSocketController {

    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Entry Point: /app/sendMessage/{roomId}
     * Frontend sends: { "senderId": "A", "content": "Hello" }
     */
    @MessageMapping("/sendMessage/{roomId}")
    public void sendMessage(
            @DestinationVariable String roomId,
            @Payload ChatMessage message
    ) {
        // 1. Enrich the Message (Trust Boundary)
        message.setRoomId(roomId); // Critical: Connects URL ID to Message Object
        message.setMessageId(UUID.randomUUID().toString());
        message.setTimestamp(Instant.now());

        log.info("MSG_IN [{}]: {}", roomId, message.getContent());

        // 2. PATH A: REAL-TIME (The Fast Path)
        // Delivers to connected subscribers via Stomp Relay
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);

        // 3. PATH B: PERSISTENCE (The Reliable Path)
        // Sends to RabbitMQ Exchange -> Queue -> Consumer -> MongoDB
        // Logic: If MongoDB is down, this won't fail; it waits in the Queue.
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.CHAT_EXCHANGE,
                    "chat.room." + roomId, // Routing Key
                    message
            );
        } catch (Exception e) {
            log.error("CRITICAL: Failed to queue message for persistence", e);
            // In a real prod system, you might send a "NACK" frame back to client here
        }
    }
}

package com.sanchar.chat_service.controller;

import com.sanchar.chat_service.config.RabbitConfig;
import com.sanchar.chat_service.model.ChatMessage;
import com.sanchar.chat_service.model.MessageAck;
import com.sanchar.chat_service.model.MessageStatus;
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
//    @MessageMapping("/sendMessage/{roomId}")
//    public void sendMessage(
//            @DestinationVariable String roomId,
//            @Payload ChatMessage message
//    ) {
//        // 1. Enrich the Message (Trust Boundary)
//        message.setRoomId(roomId); // Critical: Connects URL ID to Message Object
//        message.setMessageId(UUID.randomUUID().toString());
//        message.setTimestamp(Instant.now());
//
//        log.info("MSG_IN [{}]: {}", roomId, message.getContent());
//
//        // 2. PATH A: REAL-TIME (The Fast Path)
//        // Delivers to connected subscribers via Stomp Relay
//        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
//
//        // 3. PATH B: PERSISTENCE (The Reliable Path)
//        // Sends to RabbitMQ Exchange -> Queue -> Consumer -> MongoDB
//        // Logic: If MongoDB is down, this won't fail; it waits in the Queue.
//        try {
//            rabbitTemplate.convertAndSend(
//                    RabbitConfig.CHAT_EXCHANGE,
//                    "chat.room." + roomId, // Routing Key
//                    message
//            );
//        } catch (Exception e) {
//            log.error("CRITICAL: Failed to queue message for persistence", e);
//            // In a real prod system, you might send a "NACK" frame back to client here
//        }
//    }
//    @MessageMapping("/sendMessage/{roomId}")
//    public void sendMessage(
//            @DestinationVariable String roomId,
//            @Payload ChatMessage message
//    ) {
//        // 1. Enrich the Message (Trust Boundary)
//        // Ensure ID and Timestamp are set by server, not client
//        message.setRoomId(roomId);
//        message.setMessageId(UUID.randomUUID().toString());
//        message.setTimestamp(Instant.now());
//
//        log.info("MSG_IN [{}]: {}", roomId, message.getContent());
//
//        // -------------------------------------------------------------
//        // PATH A: REAL-TIME (Hot Path)
//        // -------------------------------------------------------------
//
//        // 2a. Broadcast to the Room (For people currently LOOKING at this chat)
//        // Subscribers: /topic/room/{roomId}
//        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
//
//        // 2b. Broadcast to Notification Channel (NEW ADDITION)
//        // For updating the "Sidebar / Inbox List" if the user is elsewhere.
//        // Subscribers: /topic/user/{userId}
//        if (message.getRecipientId() != null) {
//            // Notify the Recipient (So their phone/browser updates notifications)
//            messagingTemplate.convertAndSend("/topic/user/" + message.getRecipientId(), message);
//
//            // Notify the Sender (So my other devices update their sidebar instantly too)
//            messagingTemplate.convertAndSend("/topic/user/" + message.getSenderId(), message);
//        }
//
//        // -------------------------------------------------------------
//        // PATH B: PERSISTENCE (Cold Path)
//        // -------------------------------------------------------------
//
//        // 3. Send to RabbitMQ Exchange -> Queue -> Consumer -> MongoDB
//        try {
//            rabbitTemplate.convertAndSend(
//                    RabbitConfig.CHAT_EXCHANGE,
//                    "chat.room." + roomId, // Routing Key matches "chat.room.#" binding
//                    message
//            );
//        } catch (Exception e) {
//            log.error("CRITICAL: Failed to queue message for persistence", e);
//        }
//    }
    @MessageMapping("/sendMessage/{roomId}")
    public void sendMessage(
            @DestinationVariable String roomId,
            @Payload ChatMessage message
    ) {
        // 1. Enrich Logic
        message.setRoomId(roomId);
        message.setMessageId(UUID.randomUUID().toString());
        message.setTimestamp(Instant.now());
        message.setStatus(MessageStatus.SENT);
        log.info("MSG_IN [{}]: {}", roomId, message.getContent());

        // -------------------------------------------------------------
        // PATH A: REAL-TIME (Dot Notation Fix)
        // -------------------------------------------------------------

        // 2a. Broadcast to Room
        // Previous: "/topic/room/" + roomId
        // NEW:      "/topic/room." + roomId  (Aligns with Frontend)
        messagingTemplate.convertAndSend("/topic/room." + roomId, message);

        // 2b. Broadcast to User Inboxes (Notification Channel)
        if (message.getRecipientId() != null) {
            // Notify Recipient (e.g., /topic/user.123)
            messagingTemplate.convertAndSend("/topic/user." + message.getRecipientId(), message);

            // Notify Sender (so their other tabs/devices update the sidebar)
            messagingTemplate.convertAndSend("/topic/user." + message.getSenderId(), message);
        }

        // -------------------------------------------------------------
        // PATH B: PERSISTENCE (RabbitMQ AMQP)
        // -------------------------------------------------------------
        // Note: The Persistence consumer uses AMQP bindings set in RabbitConfig.
        // We typically keep using "chat.room.{id}" here (which uses dots already).
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.CHAT_EXCHANGE,
                    "chat.room." + roomId,
                    message
            );
        } catch (Exception e) {
            log.error("Persistence Queue Error", e);
        }
    }

    @MessageMapping("/ack/{roomId}")
    public void acknowledgeMessage(
            @DestinationVariable String roomId,
            @Payload MessageAck ack
    ) {
        log.info("ACK: Msg [{}] is [{}] by User [{}]", ack.getMessageId(), ack.getStatus(), ack.getRecipientId());

        // 1. NOTIFY THE ORIGINAL SENDER
        // We send this specific Ack packet to the User's private Notification topic.
        // The frontend listens to "/topic/user.{myId}" -> finds msg in UI -> changes tick color.

        if (ack.getSenderId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/user." + ack.getSenderId(),
                    ack
            );
        }

        // 2. LOGIC FOR 'SEEN' (OPTIONAL DB SYNC)
        // Ideally, 'SEEN' should also update the "lastReadAt" field in Mongo
        // to sync across devices, but usually that is done via the HTTP POST /mark-read endpoint
        // to avoid spamming the database with socket frames.
        // For MVP, Real-time echo is enough.
        rabbitTemplate.convertAndSend(RabbitConfig.CHAT_EXCHANGE, "chat.ack." + roomId, ack);
//        if (ack.getStatus() == MessageStatus.SEEN) {
//            log.debug("Message {} marked as seen in realtime.", ack.getMessageId());
//        }
    }

}

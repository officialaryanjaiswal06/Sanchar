package com.sanchar.chat_service.service;
import com.mongodb.client.result.UpdateResult;
import com.sanchar.chat_service.config.RabbitConfig;
import com.sanchar.chat_service.model.ChatBucket;
import com.sanchar.chat_service.model.ChatMessage;
import com.sanchar.chat_service.model.Conversation;
import com.sanchar.chat_service.model.MessageAck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@Slf4j
@RequiredArgsConstructor
@RabbitListener(queues = RabbitConfig.DB_QUEUE)
public class ChatConsumerService {

    private final MongoTemplate mongoTemplate;

//    @RabbitListener(queues = RabbitConfig.DB_QUEUE)
    @RabbitHandler
    public void persistMessage(ChatMessage message) {
        try {
            saveToBucket(message);

            updateInbox(message);
        } catch (Exception e) {
            log.error("Failed to persist message ID: {}. Reason: {}", message.getMessageId(), e.getMessage());
            // Throwing allows RabbitMQ to requeue/dead-letter the message (Reliability)
            throw e;
        }
    }

    @RabbitHandler
    @Retryable(
            value = {MessageNotFoundException.class},
            maxAttempts = 5,           // Try 5 times
            backoff = @Backoff(delay = 1000) // Wait 1 second between tries
    )
    public void handleAck(MessageAck ack) {
        log.info("Processing Ack: {} -> {}", ack.getStatus(), ack.getMessageId());

        // 1. Build Query: Match Room AND the specific Message inside array
        Query query = Query.query(Criteria.where("rid").is(ack.getRoomId())
                .and("msgs.mid").is(ack.getMessageId()));

        // 2. Positional Update ($): Set status of matched item
        Update update = new Update().set("msgs.$.st", ack.getStatus());

        // 3. Execute
        UpdateResult result = mongoTemplate.updateFirst(query, update, ChatBucket.class);

        // 4. VERIFY: Did we actually find the message?
        if (result.getMatchedCount() == 0) {
            log.warn("⏳ Race Condition: Message {} not found in DB yet. Retrying Ack...", ack.getMessageId());
            throw new MessageNotFoundException(); // Triggers Retry
        }

        log.info("✅ Status updated to {} for msg {}", ack.getStatus(), ack.getMessageId());
    }

    private void saveToBucket(ChatMessage message) {
        // 1. Calculate Bucket ID based on Time (Daily Partitioning)
        // Example: "123_2025-12-28"
        LocalDate today = LocalDate.ofInstant(message.getTimestamp(), ZoneId.of("UTC"));
        String bucketId = message.getRoomId() + "_" + today.toString();

        // 2. Prepare Atomic Upsert
        Query query = Query.query(Criteria.where("_id").is(bucketId));

        Update update = new Update();
        update.push("msgs", message); // $push the message to array
        update.setOnInsert("rid", message.getRoomId()); // Only set rid if new bucket

        // 3. Execute
        // findAndModify is Thread-Safe in MongoDB
        mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().upsert(true), // Create if missing
                ChatBucket.class
        );

        log.debug("Persisted message {} to bucket {}", message.getMessageId(), bucketId);
    }

    private void updateInbox(ChatMessage msg) {
        // Only if it's a private chat (has recipient). Group logic is different.
        if (msg.getSenderId() != null && msg.getRecipientId() != null) {
            // My Inbox: "I sent..."
            upsertConversation(msg.getSenderId(), msg.getRecipientId(), msg);
            // Their Inbox: "They sent..."
            upsertConversation(msg.getRecipientId(), msg.getSenderId(), msg);
        }
    }

    private void upsertConversation(String owner, String friend, ChatMessage msg) {
        String id = owner + "_" + friend;
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("userId", owner);
        update.set("otherUserId", friend);
        update.set("roomId", msg.getRoomId());
        update.set("lastMessage", msg.getContent());
        update.set("lastMessageSenderId", msg.getSenderId());
        update.set("lastMessageTime", msg.getTimestamp());

        mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().upsert(true), Conversation.class);
    }
    public static class MessageNotFoundException extends RuntimeException {}
}

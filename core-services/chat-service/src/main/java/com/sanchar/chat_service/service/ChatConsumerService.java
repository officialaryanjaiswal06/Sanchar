package com.sanchar.chat_service.service;
import com.sanchar.chat_service.config.RabbitConfig;
import com.sanchar.chat_service.model.ChatBucket;
import com.sanchar.chat_service.model.ChatMessage;
import com.sanchar.chat_service.model.Conversation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatConsumerService {

    private final MongoTemplate mongoTemplate;

    @RabbitListener(queues = RabbitConfig.DB_QUEUE)
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
        update.set("lastMessageTime", msg.getTimestamp());

        mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().upsert(true), Conversation.class);
    }
}

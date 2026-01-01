package com.sanchar.chat_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage implements Serializable {
    @Field("mid")
    private String messageId; // UUID

    @Field("rid")
    private String roomId;

    @Field("s")
    private String senderId;

    @Field("r")
    private String recipientId; // Can be null for Group chats

    @Field("c")
    private String content;

    @Field("ts")
    private Instant timestamp;
}

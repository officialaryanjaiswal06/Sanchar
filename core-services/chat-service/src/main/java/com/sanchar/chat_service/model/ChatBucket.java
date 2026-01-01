package com.sanchar.chat_service.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_buckets")
// Index optimized for fetching history: Give me buckets for Room X, sorted by newest first
@CompoundIndex(name = "room_time_idx", def = "{'rid': 1, 'id': -1}")
public class ChatBucket {
    @Id
    private String id; // Logic: roomId + "_" + roundedTimestamp

    @Field("rid")
    private String roomId; // Shortened key to save storage

    @Builder.Default
    @Field("msgs")
    private List<ChatMessage> messages = new ArrayList<>();
}

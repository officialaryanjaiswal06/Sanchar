package com.sanchar.chat_service.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "user_conversations")
@CompoundIndex(name = "user_recent_idx", def = "{'userId': 1, 'lastMessageTime': -1}")
public class Conversation {

    @Id
    private String id; // Format: OwnerID_FriendID (Unique per view)

    private String userId;        // Me
    private String otherUserId;   // Them
    private String roomId;

    private String lastMessageSenderId;

    private String lastMessage;
    private Instant lastMessageTime;
}

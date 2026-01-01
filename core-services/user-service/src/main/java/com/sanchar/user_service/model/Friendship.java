package com.sanchar.user_service.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "friendships")

@CompoundIndex(name = "unique_friendship", def = "{'requesterId': 1, 'addresseeId': 1}", unique = true)
public class Friendship {
    @Id
    private String id;

    private String requesterId;
    private String addresseeId;

    private FriendStatus status; // PENDING, ACCEPTED, REJECTED

    // Determine the shared Room ID (Logic: alphabetically sorted IDs)
    // We store this once established so both users know where to chat
    private String roomId;

    private LocalDateTime createdAt;
}

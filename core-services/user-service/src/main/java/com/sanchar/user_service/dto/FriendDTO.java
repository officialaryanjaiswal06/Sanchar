package com.sanchar.user_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendDTO {
    private String requestId;
    private String friendId;
    private String username;
    private String fullName;
    private String profilePicture;
    private String roomId;
}

package com.sanchar.user_service.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String gender;
    private String bio;
    private String profilePictureUrl;
    private LocalDateTime joinedAt;
}

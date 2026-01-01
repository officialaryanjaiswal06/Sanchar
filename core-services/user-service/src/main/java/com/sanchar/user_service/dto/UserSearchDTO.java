package com.sanchar.user_service.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchDTO {
    private String userId;
    private String username;
    private String fullName;
    private String profilePicture;

}

package com.sanchar.user_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {
    private String userId;
    private String email;
    private String message;
}

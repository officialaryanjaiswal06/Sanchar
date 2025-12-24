package com.sanchar.user_service.dto;

import com.sanchar.user_service.model.Gender;
import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String username;
    private String email;
    private String password;
    private String bio;
    private Gender gender;
}

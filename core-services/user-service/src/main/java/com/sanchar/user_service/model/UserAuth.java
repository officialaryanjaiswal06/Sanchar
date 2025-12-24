package com.sanchar.user_service.model;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "user_auth")
public class UserAuth {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String password; // BCrypt Hash

    @Builder.Default
    private boolean isAccountEnabled = false; // False until OTP verified

    @Builder.Default
    private boolean isLocked = false;

    private String otp;
    private LocalDateTime otpExpiry;
    private OtpType otpType;

    @Builder.Default
    private int attemptCount = 0;

    private String refreshTokenId;
}

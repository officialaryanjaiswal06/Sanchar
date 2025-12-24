package com.sanchar.common_library.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisteredEvent implements Serializable {

    private String userId;
    private String email;
    private String fullName;
    private String username;
    private String otpCode;
}

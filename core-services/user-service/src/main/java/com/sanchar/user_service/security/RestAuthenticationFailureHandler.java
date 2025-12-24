package com.sanchar.user_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.sanchar.common_library.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final ObjectMapper objectMapper;

    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res, AuthenticationException e)
            throws IOException {
        res.setStatus(HttpStatus.UNAUTHORIZED.value());
        res.setContentType("application/json");
        String msg = e.getMessage().contains("Disabled") ? "Account Not Verified. Check OTP." : "Invalid Credentials";
        res.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(msg, HttpStatus.UNAUTHORIZED, e.getMessage())));
    }
}

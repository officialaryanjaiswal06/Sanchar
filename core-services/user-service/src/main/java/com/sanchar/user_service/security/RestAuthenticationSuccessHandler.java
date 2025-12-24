package com.sanchar.user_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanchar.common_library.dto.ApiResponse;
import com.sanchar.common_library.utils.JwtUtils;
import com.sanchar.user_service.dto.AuthResponse;

import com.sanchar.user_service.model.User;
import com.sanchar.user_service.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RestAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${jwt.secret}") private String jwtSecret;
    @Value("${jwt.access.expiration}") private Long accessExp;
    @Value("${jwt.refresh.expiration}") private Long refreshExp;



    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
//        String username = ((User) auth.getPrincipal()).getUsername();
        UserDetails principal = (UserDetails) auth.getPrincipal();
        String username = principal.getUsername();
//        var domainUser = userRepository.findByUsername(username).orElseThrow();

        User domainUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User found in Context but not in DB!"));


        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", domainUser.getId());
        claims.put("email", domainUser.getEmail());

        String accessToken = jwtUtils.generateToken(username, claims, jwtSecret, accessExp);
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set("auth:refresh:" + domainUser.getId(), refreshToken, Duration.ofMillis(refreshExp));

        AuthResponse data = new AuthResponse(accessToken, refreshToken, domainUser.getId(), domainUser.getUsername());

        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(ApiResponse.success("Login Successful", data)));
    }
}

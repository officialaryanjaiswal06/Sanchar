package com.sanchar.user_service.security;

import com.sanchar.user_service.model.User;
import com.sanchar.user_service.model.UserAuth;
import com.sanchar.user_service.repository.UserAuthRepository;
import com.sanchar.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. First, find the Public Profile (Users Collection) to get the ID
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // 2. Next, use the ID to find the Credentials (UserAuth Collection)
        // This is where the Password and Lock status live.
        UserAuth userAuth = userAuthRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Authentication data not found for user"));

        // 3. Construct the Spring Security User object
        // We Map:
        // - username -> from User
        // - password -> from UserAuth (BCrypt Hash)
        // - disabled -> !userAuth.isAccountEnabled() (Controls OTP Block)
        // - locked   -> userAuth.isLocked() (Controls Admin Bans)
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(userAuth.getPassword())
                .disabled(!userAuth.isAccountEnabled()) // If OTP not verified, this is TRUE
                .accountLocked(userAuth.isLocked())     // If Banned, this is TRUE
                .accountExpired(false)
                .credentialsExpired(false)
                .authorities(Collections.emptyList())   // We can add "ROLE_USER" here later
                .build();
    }
}

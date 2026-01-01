package com.sanchar.user_service.config;

import com.sanchar.user_service.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    // CHANGE 1: Use InternalFilter instead of JwtAuthenticationFilter
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthEntryPoint unauthorizedHandler;
    private final RestAuthenticationSuccessHandler successHandler;
    private final RestAuthenticationFailureHandler failureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Auth routes are open (handled by InternalFilter bypass + AuthController)
                        .requestMatchers("/auth/**").permitAll()

                        // 2. INTERNAL Endpoints: now REQUIRE 'authenticated()'
                        // Authentication comes from InternalFilter processing the Header.
//                        .requestMatchers("/users/**").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/users/**").permitAll()
//                                .requestMatchers("/friends/**").authenticated()
                                .requestMatchers("/friends/internal/**").permitAll()
                                // 3. Catch all
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                // Form Login configuration allows the /auth/login endpoint to generate JWTs
                .formLogin(form -> form
                        .loginProcessingUrl("/auth/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                // CHANGE 2: Add InternalFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // Keep Authentication beans (Needed for Login functionality)
    @Bean public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }
    @Bean public AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
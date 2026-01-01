package com.sanchar.chat_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
public class InternalFeignConfig {

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Internal-Secret", gatewaySecret);
            // System-to-System call identification
            requestTemplate.header("X-Authenticated-User", "system-chat-service");
        };
    }
}

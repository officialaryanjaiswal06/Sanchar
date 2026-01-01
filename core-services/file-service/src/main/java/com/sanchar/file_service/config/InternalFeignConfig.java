package com.sanchar.file_service.config;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalFeignConfig {
    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Inject the Trust Secret into every Feign Call
            requestTemplate.header("X-Internal-Secret", gatewaySecret);

            // Also inject a trusted system User ID (e.g. "system") so User Service context works
            // Or extract it from the current context if forwarding user requests
            requestTemplate.header("X-Authenticated-User", "system-file-service");
        };
    }
}

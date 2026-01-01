package com.sanchar.api_gateway.filter;
import com.sanchar.api_gateway.config.RouteValidator;
import com.sanchar.common_library.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
// Naming Convention: Starts with "Authentication", ends with "GatewayFilterFactory"
// In application.properties, you will refer to this simply as "Authentication"
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${gateway.secret}")
    private String gatewaySecret;

    public AuthenticationGatewayFilterFactory() {
        super(Config.class);
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Check if Endpoint is Secured
            if (validator.isSecured.test(request)) {

                // 2. Missing Header
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                } else {
                    // Bad Format
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                // 3. Validation Logic
                try {
                    // Calls common-library validation
                    if (jwtUtils.validateToken(authHeader, jwtSecret)) {

                        String userId = jwtUtils.extractUsername(authHeader, jwtSecret);

                        // 4. Inject Trust Headers (Gateway Offloading)
                        request = exchange.getRequest()
                                .mutate()
                                .header("X-Authenticated-User", userId)
                                .header("X-Internal-Secret", gatewaySecret)
                                .build();

                    } else {
                        // Token Logic Invalid (Expired/Bad Signature)
                        System.out.println("❌ Token Validation Failed");
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                } catch (Exception e) {
                    System.out.println("⛔ Security Exception: " + e.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }

            // 5. Forward Request
            return chain.filter(exchange.mutate().request(request).build());
        });
    }
}
//package com.sanchar.user_service.security;
//
//import com.sanchar.common_library.utils.JwtUtils;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.NonNull;
//import lombok.RequiredArgsConstructor;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.List;
//
//
//@Component
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    @Value("${gateway.secret}") // Reads "sanchar-super-secret..." from application.properties
//    private String expectedSecret;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
//            throws ServletException, IOException {
//
//        // 1. BYPASS PUBLIC AUTH ENDPOINTS
//        // The Gateway's RouteValidator passes these through without security headers.
//        // We let them pass here too, so your AuthController can handle Login/Register logic.
//        if (request.getRequestURI().startsWith("/auth/")) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        // 2. CHECK TRUSTED HEADERS
//        String incomingSecret = request.getHeader("X-Internal-Secret");
//        String userId = request.getHeader("X-Authenticated-User");
//
//        // 3. VALIDATE HANDSHAKE
//        if (expectedSecret.equals(incomingSecret) && userId != null) {
//
//            // 4. AUTO-LOGIN (Trust the Gateway)
//            // We blindly create a SecurityContext because Gateway already checked the JWT.
//            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
//                    userId,
//                    null,
//                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
//            );
//            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//            SecurityContextHolder.getContext().setAuthentication(auth);
//
//            // Pass to Controller
//            chain.doFilter(request, response);
//        } else {
//            // 5. BLOCK UNTRUSTED TRAFFIC (e.g. Direct IP Access)
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            response.getWriter().write("Access Denied: Untrusted Source");
//        }
//    }
//}

package com.sanchar.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j; // Ensure you have logging or use System.out
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter { // Logic is actually "InternalTrust"

    @Value("${gateway.secret}")
    private String expectedSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Debug: See what URL is being hit
        String uri = request.getRequestURI();

        // 1. BYPASS PUBLIC AUTH ENDPOINTS
        if (uri.startsWith("/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. EXTRACT HEADERS
        String incomingSecret = request.getHeader("X-Internal-Secret");
        String userId = request.getHeader("X-Authenticated-User");

        // 🚨 DEBUG LOGS: THIS WILL SOLVE YOUR ERROR 🚨
        System.out.println(">>> [Filter] Request URI: " + uri);
        System.out.println(">>> [Filter] Expected Secret: " + expectedSecret);
        System.out.println(">>> [Filter] Incoming Secret: " + incomingSecret);
        System.out.println(">>> [Filter] User ID Header:  " + userId);

        // 3. VALIDATE HANDSHAKE
        if (expectedSecret.equals(incomingSecret) && userId != null) {
            System.out.println(">>> [Filter] ✅ SUCCESS: Trust Established.");

            // 4. AUTO-LOGIN
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);
        } else {
            // 5. BLOCK
            System.out.println(">>> [Filter] ❌ FAIL: Secret Mismatch or Missing User ID");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Access Denied: Untrusted Source");
        }
    }
}
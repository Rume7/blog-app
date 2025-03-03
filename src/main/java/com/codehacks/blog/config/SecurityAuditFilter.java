package com.codehacks.blog.config;

import com.codehacks.blog.model.SecurityEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SecurityAuditFilter extends OncePerRequestFilter {

    private static final List<String> SENSITIVE_PATHS = Arrays.asList(
            "/change-role",
            "/admin-only",
            "/delete-account"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getServletPath();

        if (SENSITIVE_PATHS.stream().anyMatch(requestPath::equalsIgnoreCase)) {
            logSecurityEvent(request);
        }

        filterChain.doFilter(request, response);
    }

    private void logSecurityEvent(HttpServletRequest request) {
        SecurityEvent securityEvent = SecurityEvent.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .clientIp(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .username(getAuthenticatedUsername())
                .build();
        System.out.println("Security Event: " + securityEvent);
    }

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : "ANONYMOUS"; // ✅ Default value for unauthenticated users
    }
}
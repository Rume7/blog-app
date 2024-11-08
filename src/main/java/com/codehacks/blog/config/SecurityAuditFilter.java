package com.codehacks.blog.config;

import com.codehacks.blog.model.SecurityEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityAuditFilter extends OncePerRequestFilter {

    private static final List<String> SENSITIVE_PATHS = Arrays.asList(
            "/change-role",
            "/admin-only",
            "/delete-account"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        if (SENSITIVE_PATHS.stream().anyMatch(requestPath::contains)) {
            logSecurityEvent(request);
        }

        filterChain.doFilter(request, response);
    }

    private void logSecurityEvent(HttpServletRequest request) {
        SecurityEvent event = SecurityEvent.builder()
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .clientIp(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .username(SecurityContextHolder.getContext().getAuthentication().getName())
                .build();
    }
}
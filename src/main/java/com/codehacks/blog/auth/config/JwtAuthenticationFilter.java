package com.codehacks.blog.auth.config;

import com.codehacks.blog.auth.service.CustomUserDetailsService;
import com.codehacks.blog.auth.service.TokenServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenServiceImpl tokenService;
    private final CustomUserDetailsService userDetailsService;

    private final String[] PUBLIC_PATHS = {"/api/v1/auth/register", "/api/v1/auth/login"};


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String requestURI = request.getRequestURI();
            log.debug("Processing request: {}", requestURI);

            // Skip authentication for registration and login endpoints
            AntPathMatcher pathMatcher = new AntPathMatcher();
            boolean isPublicPath = Arrays.stream(PUBLIC_PATHS)
                    .anyMatch(path -> pathMatcher.match(path, requestURI));

            if (isPublicPath) {
                log.debug("Skipping authentication for public endpoint: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            final String authHeader = request.getHeader("Authorization");
            log.debug("Authorization header: {}", authHeader != null ? "present" : "missing");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("No valid Authorization header found, proceeding with chain");
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(7);
            log.debug("Validating JWT token");

            if (tokenService.validateToken(jwt)) {
                Long userId = tokenService.getUserIdFromToken(jwt);
                String userEmailFromToken = tokenService.getUserEmailFromToken(jwt);
                log.debug("Token valid for user: {}", userEmailFromToken);

                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userId.toString());
                log.debug("User details loaded: {}", userDetails.getUsername());

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authentication set in SecurityContext");
            } else {
                log.debug("Token validation failed");
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Error processing authentication", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        }
    }
}
package com.codehacks.blog.auth.config;

import com.codehacks.blog.auth.service.UserDetailsServiceImpl;
import com.codehacks.blog.util.Constants;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Order(1)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${spring.web.cors.allowed-origins:*}")
    private String allowedOrigins;

    private static final String[] WHITELIST = {
            "/api/v1/auth/**",
            "/v2/api-docs",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui/**",
            "/webjars/**",
            "/swagger-ui.html"
    };

    private static final String[] SUBSCRIPTION_PUBLIC_ENDPOINTS = {
            Constants.SUBSCRIPTION_PATH + "/subscribe",
            Constants.SUBSCRIPTION_PATH + "/unsubscribe",
            Constants.SUBSCRIPTION_PATH + "/resubscribe"
    };

    private static final String[] SUBSCRIPTION_AUTHENTICATED_ENDPOINTS = {
            Constants.SUBSCRIPTION_PATH + "/active",
            Constants.SUBSCRIPTION_PATH + "/grouped-by-status",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.debug("Configuring SecurityFilterChain");

        http
                // CSRF protection is disabled because this is a stateless REST API using JWT authentication.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) -> {
                            log.error("Unauthorized error: {}", ex.getMessage());
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\": \"" + ex.getMessage() + "\"}");
                        })
                )
                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers(WHITELIST).permitAll()
                            .requestMatchers(HttpMethod.POST, SUBSCRIPTION_PUBLIC_ENDPOINTS).permitAll()
                            .requestMatchers(HttpMethod.GET, SUBSCRIPTION_AUTHENTICATED_ENDPOINTS[0]).hasRole("ADMIN")
                            .requestMatchers(HttpMethod.PUT, SUBSCRIPTION_AUTHENTICATED_ENDPOINTS[1]).hasRole("ADMIN")
                            .requestMatchers(HttpMethod.POST, Constants.AUTH_PATH + "/register").permitAll()
                            .requestMatchers(HttpMethod.POST, Constants.AUTH_PATH + "/login").permitAll()
                            .requestMatchers("/error").permitAll()
                            .requestMatchers(HttpMethod.PUT, Constants.AUTH_PATH + "/change-role").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.GET, "/api/v1/users").hasAuthority("ADMIN")
                            .requestMatchers(Constants.AUTH_PATH + "/logout").authenticated()
                            .requestMatchers(HttpMethod.GET, Constants.BLOG_PATH + "/all").authenticated()
                            .requestMatchers(HttpMethod.GET, Constants.BLOG_PATH + "/{id}").authenticated()
                            .requestMatchers(HttpMethod.GET, Constants.BLOG_PATH + "/recent").permitAll()
                            .requestMatchers(HttpMethod.GET, Constants.BLOG_PATH + "/previews").permitAll()
                            .requestMatchers(HttpMethod.POST, Constants.BLOG_PATH + "/create").hasAnyRole("ADMIN", "AUTHOR")
                            .requestMatchers(HttpMethod.PUT, Constants.BLOG_PATH + "/update/{id}").hasAnyRole("ADMIN", "AUTHOR")
                            .requestMatchers(HttpMethod.DELETE, Constants.BLOG_PATH + "/delete/{id}").hasAnyRole("ADMIN", "AUTHOR")
                            .requestMatchers(HttpMethod.GET, Constants.BLOG_PATH + "/search").authenticated()
                            .requestMatchers(HttpMethod.GET, Constants.BLOG_PATH + "/search/author").hasAnyRole("ADMIN", "AUTHOR")
                            .requestMatchers(HttpMethod.POST, Constants.COMMENT_PATH + "/{postId}/comments").authenticated()
                            .requestMatchers(HttpMethod.PUT, Constants.COMMENT_PATH + "/update/{postId}/{commentId}").authenticated()
                            .requestMatchers(HttpMethod.GET, Constants.COMMENT_PATH + "/{id}").authenticated()
                            .requestMatchers(HttpMethod.GET, Constants.COMMENT_PATH + "/post/{postId}").authenticated()
                            .requestMatchers(HttpMethod.DELETE, Constants.COMMENT_PATH + "/delete/{id}").hasRole("ADMIN")
                            .anyRequest().authenticated();
                    log.debug("Configured authorization rules");
                })
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        log.debug("SecurityFilterChain configuration completed");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.debug("Configuring CORS");
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept",
                "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.debug("CORS configuration completed");
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
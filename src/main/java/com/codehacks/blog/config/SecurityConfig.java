package com.codehacks.blog.config;

import com.codehacks.blog.model.Role;
import org.apache.catalina.filters.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/blog/**").permitAll()
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/change-password", "/api/v1/auth/logout")
                            .hasAnyRole(Role.USER.name(), Role.SUBSCRIBER.name(), Role.ADMIN.name())
                        .requestMatchers("/api/v1/auth/delete-account", "/api/v1/auth/change-role").hasRole(Role.ADMIN.name())
                        .anyRequest().authenticated());
                //.addFilterBefore(new RateLimitFilter());

        return http.build();
    }
}

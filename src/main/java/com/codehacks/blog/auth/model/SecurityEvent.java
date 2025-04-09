package com.codehacks.blog.auth.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SecurityEvent {
    private LocalDateTime timestamp;
    private String path;
    private String method;
    private String clientIp;
    private String userAgent;
    private String username;
}

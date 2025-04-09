package com.codehacks.blog.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityService securityService;

    @Autowired
    public AdminService(RedisTemplate<String, String> redisTemplate, SecurityService securityService) {
        this.redisTemplate = redisTemplate;
        this.securityService = securityService;
    }

    public void logAdminAccess(String adminEmail, String ipAddress) {
        LocalDateTime timestamp = LocalDateTime.now();

        logger.info("Admin access detected: Email = {}, IP Address = {} at {}",
                adminEmail, ipAddress, timestamp);

        String redisKey = "admin:access:" + adminEmail;

        String accessDetails = String.format("IP: %s, Time: %s", ipAddress, timestamp);
        redisTemplate.opsForList().rightPush(redisKey, accessDetails);

        redisTemplate.expire(redisKey, java.time.Duration.ofDays(7));
    }

    public void reportUnauthorizedAdminAccess(String adminEmail, String ipAddress) {
        logger.warn("Unauthorized admin access attempt detected: Email = {}, IP Address = {} at {}",
                adminEmail, ipAddress, LocalDateTime.now());

        String redisKey = "admin:unauthorized:access:" + adminEmail;

        String accessDetails = String.format("IP: %s, Time: %s", ipAddress, LocalDateTime.now());
        redisTemplate.opsForList().rightPush(redisKey, accessDetails);

        short expiration_days_for_log = 120;
        redisTemplate.expire(redisKey, java.time.Duration.ofDays(expiration_days_for_log));

        securityService.handleSuspiciousLogin(adminEmail, ipAddress);
    }
}

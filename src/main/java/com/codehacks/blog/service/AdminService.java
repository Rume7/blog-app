package com.codehacks.blog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final RedisTemplate<String, String> redisTemplate;

    public AdminService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void logAdminAccess(String adminEmail, String ipAddress) {
        LocalDateTime timestamp = LocalDateTime.now();

        logger.info("Admin access detected: Email = {}, IP Address = {} at ",
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

        // Optionally set an expiry time for the unauthorized access logs (e.g., 30 days)
        redisTemplate.expire(redisKey, java.time.Duration.ofDays(30));

        triggerAlert(adminEmail, ipAddress, accessDetails);
    }

    private void triggerAlert(String adminEmail, String ipAddress, String accessDetails) {
        logger.error("ALERT: Unauthorized access attempt! Details = {}", accessDetails);

        // TODO: Integrate with an email or notification service to send alerts
    }
}

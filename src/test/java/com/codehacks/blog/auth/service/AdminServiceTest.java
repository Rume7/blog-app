package com.codehacks.blog.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final ListOperations<String, String> listOperations = mock(ListOperations.class);
    private final SecurityService securityService = mock(SecurityService.class);
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        adminService = new AdminServiceImpl(redisTemplate, securityService);
    }

    @Test
    void testLogAdminAccess() {
        // Given
        String adminEmail = "admin@example.com";
        String ipAddress = "192.168.1.1";
        String redisKey = "admin:access:" + adminEmail;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String formattedTime = LocalDateTime.now().format(formatter);

        // When
        adminService.logAdminAccess(adminEmail, ipAddress);

        // Then
        ArgumentCaptor<String> captorKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> captorValue = ArgumentCaptor.forClass(String.class);

        verify(listOperations).rightPush(captorKey.capture(), captorValue.capture());
        verify(redisTemplate).expire(eq(redisKey), eq(java.time.Duration.ofDays(7)));

        assertEquals(redisKey, captorKey.getValue());
        String loggedValue = captorValue.getValue();
        loggedValue = loggedValue.substring(0, loggedValue.lastIndexOf('.'));
        assertEquals(String.format("IP: %s, Time: %s", ipAddress, formattedTime), loggedValue);
    }

    @Test
    void testReportUnauthorizedAdminAccess() {
        // Given
        String adminEmail = "hacker@example.com";
        String ipAddress = "203.0.113.99";
        String redisKey = "admin:unauthorized:access:" + adminEmail;

        try (MockedStatic<LoggerFactory> mockedLoggerFactory = mockStatic(LoggerFactory.class)) {
            Logger mockLogger = mock(Logger.class);
            mockedLoggerFactory.when(() -> LoggerFactory.getLogger(AdminService.class)).thenReturn(mockLogger);

            adminService = new AdminServiceImpl(redisTemplate, securityService);

            // When
            adminService.reportUnauthorizedAdminAccess(adminEmail, ipAddress);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

            verify(listOperations).rightPush(keyCaptor.capture(), valueCaptor.capture());
            verify(redisTemplate).expire(eq(redisKey), eq(java.time.Duration.ofDays(120)));
        }
    }
}
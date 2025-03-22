package com.codehacks.blog.service;

import com.codehacks.blog.config.MailConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityServiceTest {

    @Mock
    private MailConfig mailConfig;

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private SecurityService securityService;

    private MailConfig mailConfigMock;

    private JavaMailSender mailSenderMock;

    private static final String TEST_EMAIL = "user@company.com";
    private static final String TEST_IP = "192.168.1.1";
    private static final String KNOWN_IP = "10.0.0.1";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mailConfigMock = mock(MailConfig.class);
        mailSenderMock = mock(JavaMailSender.class);

        when(mailConfig.javaMailSender()).thenReturn(javaMailSender);
    }

    @Test
    void testHandleSuspiciousLogin_ValidSuspiciousLogin() throws IllegalAccessException, NoSuchFieldException {
        // Given
        when(mailConfigMock.javaMailSender()).thenReturn(mailSenderMock);

        Set<String> knownIps = Set.of(KNOWN_IP, "10.0.0.2");
        SecurityService securityService = new SecurityService(mailConfigMock, knownIps);

        Field securityEmailField = SecurityService.class.getDeclaredField("securityEmail");
        securityEmailField.setAccessible(true);
        securityEmailField.set(securityService, "security@example.com");

        // When: Calling handleSuspiciousLogin (should invoke sendEmail)
        securityService.handleSuspiciousLogin(TEST_EMAIL, TEST_IP);

        // Then
        verify(mailSenderMock).send(any(SimpleMailMessage.class));
    }

    @Test
    void testHandleSuspiciousLogin_KnownIp() {
        // Given
        Set<String> knownIps = Set.of(TEST_IP, KNOWN_IP);
        securityService = new SecurityService(mailConfigMock, knownIps);

        // When
        when(mailConfigMock.javaMailSender()).thenReturn(mailSenderMock);
        securityService.handleSuspiciousLogin(TEST_EMAIL, TEST_IP);

        // Then
        verify(mailSenderMock, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testTriggerAlert_InvalidEmail() throws NoSuchFieldException, IllegalAccessException {
        // Given
        Set<String> knownIps = Set.of(KNOWN_IP, "10.0.0.2");
        SecurityService securityService = new SecurityService(mailConfigMock, knownIps);

        Field securityEmailField = SecurityService.class.getDeclaredField("securityEmail");
        securityEmailField.setAccessible(true);
        securityEmailField.set(securityService, "invalid-email");

        // When
        when(mailConfigMock.javaMailSender()).thenReturn(mailSenderMock);
        securityService.handleSuspiciousLogin(TEST_EMAIL, TEST_IP);

        // Then
        verify(mailSenderMock, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendEmail_MailException() throws Exception {
        // Given
        Set<String> knownIps = Set.of(KNOWN_IP);
        Method sendEmailMethod = SecurityService.class.getDeclaredMethod("sendEmail", String.class, String.class, String.class);
        sendEmailMethod.setAccessible(true);

        // When
        doThrow(new MailException("Simulated mail exception") {}).when(mailSenderMock).send(any(SimpleMailMessage.class));
        when(mailConfigMock.javaMailSender()).thenReturn(mailSenderMock);

        SecurityService securityService = new SecurityService(mailConfigMock, knownIps);
        sendEmailMethod.invoke(securityService, "test@example.com", "Test Subject", "Test Message");

        // Then
        verify(mailSenderMock).send(any(SimpleMailMessage.class));
    }

    @Test
    void testIsValidEmail_ValidEmail() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Given
        SecurityService securityService = new SecurityService(mailConfig, Set.of(KNOWN_IP));

        Method isValidEmailMethod = SecurityService.class.getDeclaredMethod("isValidEmail", String.class);
        isValidEmailMethod.setAccessible(true);

        //When: Test with a valid email
        boolean result = (boolean) isValidEmailMethod.invoke(securityService, "user@domain.com");
        assertTrue(result);

        // Test with an invalid email
        result = (boolean) isValidEmailMethod.invoke(securityService, "user@domain");

        // Then
        assertFalse(result);
    }

    @Test
    void testIsValidEmail_InvalidEmail() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Given
        SecurityService securityService = new SecurityService(mailConfig, Set.of(KNOWN_IP));

        Method isValidEmailMethod = SecurityService.class.getDeclaredMethod("isValidEmail", String.class);
        isValidEmailMethod.setAccessible(true);

        // When
        String invalidEmail = "user@domain";
        boolean result = (boolean) isValidEmailMethod.invoke(securityService, invalidEmail);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsSuspiciousLogin_KnownIp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Given
        Set<String> knownIps = Set.of(KNOWN_IP);
        SecurityService securityService = new SecurityService(mailConfig, knownIps);

        Method isSuspiciousLoginMethod = SecurityService.class.getDeclaredMethod("isSuspiciousLogin", String.class);
        isSuspiciousLoginMethod.setAccessible(true);

        // When
        boolean result = (boolean) isSuspiciousLoginMethod.invoke(securityService, KNOWN_IP);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsSuspiciousLogin_UnknownIp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Given
        Set<String> knownIps = Set.of(KNOWN_IP);
        SecurityService securityService = new SecurityService(mailConfig, knownIps);

        Method isSuspiciousLoginMethod = SecurityService.class.getDeclaredMethod("isSuspiciousLogin", String.class);
        isSuspiciousLoginMethod.setAccessible(true);

        // When
        boolean result = (boolean) isSuspiciousLoginMethod.invoke(securityService, TEST_IP);

        // Then
        assertTrue(result);
    }
}

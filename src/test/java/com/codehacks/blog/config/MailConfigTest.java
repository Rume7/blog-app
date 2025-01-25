package com.codehacks.blog.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testcontainers.containers.GenericContainer;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class MailConfigTest {

    private static GenericContainer<?> mailContainer;
    private static final int SMTP_PORT = 25;

    @BeforeAll
    static void setUp() {
        // Start a Testcontainers instance for a mail server
        mailContainer = new GenericContainer<>("mockserver/mockserver:latest")
                .withExposedPorts(SMTP_PORT);
        mailContainer.start();

        // Set system properties to point to the Testcontainers mail server
        System.setProperty("MAIL_HOST_1", mailContainer.getHost());
        System.setProperty("MAIL_PORT_1", mailContainer.getMappedPort(SMTP_PORT).toString());
        System.setProperty("MAIL_USERNAME_1", "testuser");
        System.setProperty("MAIL_PASSWORD_1", "testpassword");
    }

    @AfterAll
    static void tearDown() {
        if (mailContainer != null) {
            mailContainer.stop();
        }
    }

    @Test
    void testJavaMailSenderBean_CorrectlyConfigured() {
        // Create a Spring application context for MailConfig
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MailConfig.class);

        // Retrieve the JavaMailSender bean
        JavaMailSender mailSender = context.getBean(JavaMailSender.class);
        assertNotNull(mailSender);
        assertTrue(mailSender instanceof JavaMailSenderImpl);

        // Validate the configuration
        JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) mailSender;

        // Check custom properties
        assertEquals(mailContainer.getHost(), mailSenderImpl.getHost());
        assertEquals(mailContainer.getMappedPort(SMTP_PORT), mailSenderImpl.getPort());
        assertEquals("testuser", mailSenderImpl.getUsername());
        assertEquals("testpassword", mailSenderImpl.getPassword());

        Properties javaMailProperties = mailSenderImpl.getJavaMailProperties();
        assertEquals("smtp", javaMailProperties.getProperty("mail.transport.protocol"));
        assertEquals("true", javaMailProperties.getProperty("mail.smtp.auth"));
        assertEquals("true", javaMailProperties.getProperty("mail.smtp.starttls.enable"));
        assertEquals("true", javaMailProperties.getProperty("mail.debug"));

        // Close the context
        context.close();
    }
}

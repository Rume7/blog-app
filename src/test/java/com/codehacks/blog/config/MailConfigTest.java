package com.codehacks.blog.config;

import com.codehacks.blog.auth.config.MailConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testcontainers.containers.GenericContainer;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MailConfigTest {

    private static GenericContainer<?> mailContainer;
    private static final int SMTP_PORT = 3025;  // GreenMail default SMTP port

    @BeforeAll
    static void setUp() {
        // Start a Testcontainers instance for a real mail server (GreenMail)
        mailContainer = new GenericContainer<>("greenmail/standalone:latest")
                .withExposedPorts(SMTP_PORT);
        mailContainer.start();

        // Set environment variables instead of system properties
        System.setProperty("spring.mail.host", mailContainer.getHost());
        System.setProperty("spring.mail.port", mailContainer.getMappedPort(SMTP_PORT).toString());
        System.setProperty("spring.mail.username", "testUser");
        System.setProperty("spring.mail.password", "testPassword");
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
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // Set environment properties
        ConfigurableEnvironment env = context.getEnvironment();
        env.getSystemProperties().put("spring.mail.host", mailContainer.getHost());
        env.getSystemProperties().put("spring.mail.port", mailContainer.getMappedPort(SMTP_PORT).toString());
        env.getSystemProperties().put("spring.mail.username", "testUser");
        env.getSystemProperties().put("spring.mail.password", "testPassword");

        context.register(MailConfig.class);
        context.refresh();

        // Retrieve the JavaMailSender bean
        JavaMailSender mailSender = context.getBean(JavaMailSender.class);
        assertNotNull(mailSender);
        assertInstanceOf(JavaMailSenderImpl.class, mailSender);

        // Validate the configuration
        JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) mailSender;

        assertEquals(mailContainer.getHost(), mailSenderImpl.getHost());
        assertEquals(mailContainer.getMappedPort(SMTP_PORT), mailSenderImpl.getPort());
        assertEquals("testUser", mailSenderImpl.getUsername());
        assertEquals("testPassword", mailSenderImpl.getPassword());

        Properties javaMailProperties = mailSenderImpl.getJavaMailProperties();
        assertEquals("smtp", javaMailProperties.getProperty("mail.transport.protocol"));
        assertEquals("true", javaMailProperties.getProperty("mail.smtp.auth"));
        assertEquals("true", javaMailProperties.getProperty("mail.smtp.starttls.enable"));
        assertEquals("false", javaMailProperties.getProperty("mail.debug"));

        context.close();
    }

    @Test
    void testJavaMailSenderBean_UsingDefaultConfiguration() {
        // Create a Spring application context for MailConfig
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // Set default system properties for mail configuration (in case the test doesn't use Testcontainers)
        System.setProperty("spring.mail.host", "localhost");
        System.setProperty("spring.mail.port", "25");
        System.setProperty("spring.mail.username", "defaultUser");
        System.setProperty("spring.mail.password", "defaultPassword");

        // Register and refresh the application context
        context.register(MailConfig.class);
        context.refresh();

        // Retrieve the JavaMailSender bean from the context
        JavaMailSender mailSender = context.getBean(JavaMailSender.class);
        assertNotNull(mailSender);
        assertInstanceOf(JavaMailSenderImpl.class, mailSender);

        // Validate the configuration of JavaMailSender
        JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) mailSender;

        // Check that the host and port are correctly set
        assertEquals("localhost", mailSenderImpl.getHost());
        assertEquals(25, mailSenderImpl.getPort());
        assertEquals("defaultUser", mailSenderImpl.getUsername());
        assertEquals("defaultPassword", mailSenderImpl.getPassword());

        // Validate the additional mail properties
        Properties javaMailProperties = mailSenderImpl.getJavaMailProperties();
        assertEquals("smtp", javaMailProperties.getProperty("mail.transport.protocol"));
        assertEquals("true", javaMailProperties.getProperty("mail.smtp.auth"));
        assertEquals("true", javaMailProperties.getProperty("mail.smtp.starttls.enable"));
        assertEquals("false", javaMailProperties.getProperty("mail.debug"));

        // Clean up context
        context.close();
    }
}
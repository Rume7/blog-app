package com.codehacks.blog.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:example@gmail.com}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(getMailHost());
        mailSender.setPort(getMailPort());
        mailSender.setUsername(getMailUsername());

        if (mailPassword == null || mailPassword.isEmpty()) {
            throw new IllegalStateException("Mail password is not set! Please configure 'spring.mail.password'.");
        }
        mailSender.setPassword(getMailPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");                   // Set to "true" only for debugging
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com"); // Avoid SSL handshake issues

        return mailSender;
    }

    private String getMailHost() {
        return mailHost;
    }

    private int getMailPort() {
        return mailPort;
    }

    private String getMailPassword() {
        return mailPassword;
    }

    private String getMailUsername() {
        return mailUsername;
    }
}

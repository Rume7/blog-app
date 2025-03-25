package com.codehacks.blog.service;

import com.codehacks.blog.config.MailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    private final MailConfig mailSender;

    @Value("${security.team}")
    private String securityEmail;

    @Value("${security.known_ips}")
    private final Set<String> knownIps;

    @Autowired
    public SecurityService(MailConfig mailSender, @Value("${security.known_ips}") Set<String> knownIps) {
        this.mailSender = mailSender;
        this.knownIps = knownIps;
    }

    public void handleSuspiciousLogin(String email, String ipAddress) {
        if (isSuspiciousLogin(ipAddress)) {
            logger.warn("Suspicious login detected: Email={} IP={}", email, ipAddress);
            triggerAlert(email, ipAddress);
        }
    }

    private void triggerAlert(String email, String ipAddress) {
        if (!isValidEmail(securityEmail)) {
            logger.error("Security email is not configured. Cannot send alert.");
            return;
        }

        String subject = "Unusual Admin Login Detected";
        String message = """
                Dear Security Team,

                We detected unusual admin login activity:

                - User Email: %s
                - IP Address: %s
                - Timestamp: %s

                Please investigate immediately.
                """.formatted(email, ipAddress, java.time.LocalDateTime.now());

        sendEmail(securityEmail, subject, message);
    }

    private void sendEmail(String to, String subject, String message) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);
            mailSender.javaMailSender().send(mailMessage);
        } catch (MailException e) {
            logger.error("Failed to send email due to mail exception: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage());
        }
    }

    private boolean isSuspiciousLogin(String ipAddress) {
        return !isKnownIp(ipAddress);
    }

    private boolean isKnownIp(String ipAddress) {
        return knownIps != null && knownIps.contains(ipAddress);
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(emailRegex);
    }
}

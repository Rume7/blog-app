package com.codehacks.blog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    private final JavaMailSender mailSender;

    @Value("${security.email}")
    private String securityEmail;

    @Autowired
    public SecurityService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void handleSuspiciousLogin(String email, String ipAddress) {
        if (isSuspiciousLogin(ipAddress)) {
            triggerAlert(email, ipAddress);
        }
    }

    private void triggerAlert(String email, String ipAddress) {
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
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }

    private boolean isSuspiciousLogin(String ipAddress) {
        return !isKnownIp(ipAddress);
    }

    private boolean isKnownIp(String ipAddress) {
        // Logic to verify IP is known or trusted (could be a list or geo-location check)
        return ipAddress.equals("192.168.1.100");
    }
}

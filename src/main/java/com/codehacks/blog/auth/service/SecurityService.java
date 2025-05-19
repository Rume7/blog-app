package com.codehacks.blog.auth.service;

public interface SecurityService {

    void handleSuspiciousLogin(String email, String ipAddress);
}


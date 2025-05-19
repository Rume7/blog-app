package com.codehacks.blog.auth.service;

public interface AdminService {

    void logAdminAccess(String adminEmail, String ipAddress);

    void reportUnauthorizedAdminAccess(String adminEmail, String ipAddress);
}
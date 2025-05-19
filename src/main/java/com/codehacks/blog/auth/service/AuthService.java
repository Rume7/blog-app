package com.codehacks.blog.auth.service;

import com.codehacks.blog.auth.dto.UserDTO;
import com.codehacks.blog.auth.model.CustomUserDetails;
import com.codehacks.blog.auth.model.Role;
import com.codehacks.blog.auth.model.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {

    String authenticate(CustomUserDetails customUserDetails);

    UserDTO registerUser(User user, String rawPassword);

    void logout(String email);

    void logAdminAccess(String email, String ipAddress);

    void changePassword(String username, String currentPassword, String newPassword);

    User changeUserRole(String username, Role role);

    void deleteUserAccount(String username);

    void reportUnauthorizedAdminAccess(String email, String ipAddress);

    boolean canUserDeleteAccount(String username, UserDetails userDetails);
}

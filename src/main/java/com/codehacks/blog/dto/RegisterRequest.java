package com.codehacks.blog.dto;

import com.codehacks.blog.model.Role;
import com.codehacks.blog.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 5, max = 25, message = "Username must be between 5 and 25 characters")
    String username,

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@#$%^&+=!]{6,}$",
            message = "Password must be at least 6 characters long and contain at least one letter, one number, and may include special characters")
    String password,

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    String email,

    String role
) {
    static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User toUser() {
        User user = new User();
        user.setUsername(username());
        user.setPassword(passwordEncoder.encode(password()));
        user.setEmail(email());
        user.setRole(role != null ? Role.valueOf(role.toUpperCase()) : Role.USER);
        user.setEnabled(true);
        return user;
    }
}

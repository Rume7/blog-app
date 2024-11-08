package com.codehacks.blog.dto;

import com.codehacks.blog.model.Role;
import com.codehacks.blog.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 5, max = 50, message = "Username must be between 5 and 15 characters")
    String username,

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
            message = "Password must be at least 8 characters long and contain at least one letter and one number")
    String password,

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    String email
) {
    public User toUser() {
        User user = new User();
        user.setUsername(username());
        user.setPassword(password());
        user.setEmail(email());
        user.setRole(Role.USER);
        user.setEnabled(true);
        return user;
    }
}

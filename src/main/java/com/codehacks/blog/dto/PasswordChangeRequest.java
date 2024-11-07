package com.codehacks.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record PasswordChangeRequest (

    @NotBlank(message = "Username is required") String username,
    
    @NotBlank(message = "Current password is required") String currentPassword,
    
    @NotBlank(message = "New password is required")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = "Password must be at least 8 characters long and contain at least one digit, " +
                    "one uppercase letter, one lowercase letter, and one special character")
    String newPassword) { }

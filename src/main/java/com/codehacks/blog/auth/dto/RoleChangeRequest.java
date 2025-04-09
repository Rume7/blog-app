package com.codehacks.blog.auth.dto;

import com.codehacks.blog.auth.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(
        @NotBlank String username,
        @NotNull Role userRole
) {
}

package com.codehacks.blog.dto;

import com.codehacks.blog.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(
    @NotBlank String username,
    @NotNull Role userRole
) {}

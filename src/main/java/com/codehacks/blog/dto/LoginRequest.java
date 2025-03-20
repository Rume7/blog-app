package com.codehacks.blog.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String email,
        @NotBlank String password
) {}

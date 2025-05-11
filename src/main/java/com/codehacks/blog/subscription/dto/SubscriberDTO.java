package com.codehacks.blog.subscription.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SubscriberDTO(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
) {
}
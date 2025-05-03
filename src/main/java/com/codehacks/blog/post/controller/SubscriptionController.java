package com.codehacks.blog.post.controller;

import com.codehacks.blog.auth.config.RateLimit;
import com.codehacks.blog.auth.dto.ApiResponse;
import com.codehacks.blog.post.dto.SubscriberDTO;
import com.codehacks.blog.post.model.Subscriber;
import com.codehacks.blog.post.service.SubscriptionService;
import com.codehacks.blog.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for managing email subscriptions.
 * <p>
 * API Design Pattern:
 * - Complex operations (create/update with multiple fields) use @RequestBody with DTOs
 * - Simple operations (state changes with single parameter) use @RequestParam
 * <p>
 * This pattern provides:
 * 1. Clear indication of operation complexity
 * 2. Simpler client implementation for basic operations
 * 3. Better maintainability as the API grows
 * 4. Follows the principle of least surprise
 */
@Slf4j
@RestController
@RequestMapping(Constants.SUBSCRIPTION_PATH)
@RequiredArgsConstructor
@Tag(name = "Subscription Management", description = "APIs for managing email subscriptions")
@Validated
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(
            summary = "Subscribe to blog updates",
            description = "Subscribe an email address to receive blog updates. Uses request body for future extensibility.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "201",
                            description = "Subscription created successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = com.codehacks.blog.auth.dto.ApiResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid email format or other validation error"
                    )
            }
    )
    @PostMapping(value = "/subscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Subscriber>> subscribe(
            @Valid @RequestBody SubscriberDTO subscriberDTO) {
        log.info("Received subscription request for email: {}", subscriberDTO.email());
        try {
            Subscriber subscriber = subscriptionService.subscribe(subscriberDTO.email());
            ApiResponse<Subscriber> response = ApiResponse.created(subscriber);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } catch (Exception e) {
            log.error("Error processing subscription for email: {}", subscriberDTO.email(), e);
            ApiResponse<Subscriber> errorResponse = ApiResponse.error("Failed to process subscription: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }

    @Operation(
            summary = "Unsubscribe from blog updates",
            description = "Unsubscribe an email address from blog updates. Uses request parameter for simple state change.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Successfully unsubscribed",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = com.codehacks.blog.auth.dto.ApiResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid email format"
                    )
            }
    )
    @PostMapping(value = "/unsubscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> unsubscribe(
            @Valid @RequestParam @Email(message = "Invalid email format") String email) {
        try {
            subscriptionService.unsubscribe(email);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.success("Successfully unsubscribed"));
        } catch (Exception e) {
            log.error("Error processing un-subscription for email: {}", email, e);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("Failed to process un-subscription: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Resubscribe to blog updates",
            description = "Resubscribe an email address to blog updates. Uses request parameter for simple state change.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Successfully resubscribed",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = com.codehacks.blog.auth.dto.ApiResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid email format"
                    )
            }
    )
    @PostMapping(value = "/resubscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(maxRequests = 1, timeWindowMinutes = 1440) // 1 request per day
    public ResponseEntity<ApiResponse<Subscriber>> resubscribe(
            @Valid @RequestParam @Email(message = "Invalid email format") String email) {
        log.info("Received re-subscription request for email: {}", email);
        try {
            subscriptionService.resubscribe(email);
            Subscriber subscriber = subscriptionService.getActiveSubscribers().stream()
                    .filter(s -> s.getEmail().equals(email))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Subscriber not found after re-subscription"));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.success(subscriber));
        } catch (Exception e) {
            log.error("Error processing re-subscription for email: {}", email, e);
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("Failed to process re-subscription: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Get all active subscribers",
            description = "Retrieve a list of all active subscribers. Requires ADMIN role.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved active subscribers",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = com.codehacks.blog.auth.dto.ApiResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = "Not authorized to access this resource"
                    )
            }
    )
    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<Subscriber>>> getActiveSubscribers() {
        log.info("Retrieving all active subscribers");
        try {
            List<Subscriber> subscribers = subscriptionService.getActiveSubscribers();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.success(subscribers));
        } catch (Exception e) {
            log.error("Error retrieving active subscribers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("Failed to retrieve subscribers: " + e.getMessage()));
        }
    }
}

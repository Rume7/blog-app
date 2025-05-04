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

        Subscriber subscriber = subscriptionService.subscribe(subscriberDTO.email());
        ApiResponse<Subscriber> response = ApiResponse.created(subscriber);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
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
        subscriptionService.unsubscribe(email);

        return ResponseEntity.ok()
                .body(ApiResponse.success("Successfully unsubscribed"));
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
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Subscriber not found"
                    )
            }
    )
    @PostMapping(value = "/resubscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    //@RateLimit(maxRequests = 1, timeWindowMinutes = 1440) // 1 request per day
    public ResponseEntity<ApiResponse<Subscriber>> resubscribe(
            @Valid @RequestParam @Email(message = "Invalid email format") String email) {
        log.info("Received re-subscription request for email: {}", email);

        Subscriber subscriber = subscriptionService.resubscribe(email);

        return ResponseEntity.ok()
                .body(ApiResponse.success(subscriber));
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

        List<Subscriber> subscribers = subscriptionService.getActiveSubscribers();

        return ResponseEntity.ok()
                .body(ApiResponse.success(subscribers));
    }
}

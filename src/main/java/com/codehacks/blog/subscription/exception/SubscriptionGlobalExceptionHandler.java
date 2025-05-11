package com.codehacks.blog.subscription.exception;

import com.codehacks.blog.auth.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice("com.codehacks.blog.post")
@Slf4j
public class SubscriptionGlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionGlobalExceptionHandler.class);

    @ExceptionHandler(SubscriberNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleSubscriberNotFound(SubscriberNotFoundException ex) {
        logger.error("Subscriber not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // 409 - Duplicate subscription
    @ExceptionHandler(DuplicateSubscriptionException.class)
    public ResponseEntity<ApiResponse<String>> handleDuplicateSubscription(DuplicateSubscriptionException ex) {
        logger.error("Duplicate subscription attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleConstraintViolation(ConstraintViolationException ex) {
        logger.error("Constraint violation: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid input: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleAllUncaughtException(Exception ex, WebRequest request) {
        logger.error("Unhandled exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}

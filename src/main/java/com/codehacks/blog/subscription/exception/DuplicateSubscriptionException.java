package com.codehacks.blog.subscription.exception;

public class DuplicateSubscriptionException extends RuntimeException {

    public DuplicateSubscriptionException(String message) {
        super(message);
    }

    public DuplicateSubscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}


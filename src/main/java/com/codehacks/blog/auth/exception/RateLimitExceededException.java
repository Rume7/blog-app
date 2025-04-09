package com.codehacks.blog.auth.exception;

public class RateLimitExceededException extends Throwable {

    public RateLimitExceededException(String message) {
        super(message);
    }
}

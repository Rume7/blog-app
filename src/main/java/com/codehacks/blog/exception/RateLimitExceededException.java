package com.codehacks.blog.exception;

public class RateLimitExceededException extends Throwable {

    public RateLimitExceededException(String message) {
        super(message);
    }
}

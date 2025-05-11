package com.codehacks.blog.subscription.exception;

public class SubscriberNotFoundException extends RuntimeException {

    public SubscriberNotFoundException(String message) {
        super(message);
    }
}

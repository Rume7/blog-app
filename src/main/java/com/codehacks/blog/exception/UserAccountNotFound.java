package com.codehacks.blog.exception;

public class UserAccountNotFound extends RuntimeException {

    public UserAccountNotFound(String message) {
        super(message);
    }
}

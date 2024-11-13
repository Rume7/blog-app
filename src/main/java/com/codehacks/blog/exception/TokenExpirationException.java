package com.codehacks.blog.exception;

public class TokenExpirationException extends Throwable {

    public TokenExpirationException(String expiredToken) {
        super(expiredToken);
    }
}

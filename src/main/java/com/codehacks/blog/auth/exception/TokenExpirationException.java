package com.codehacks.blog.auth.exception;

public class TokenExpirationException extends Throwable {

    public TokenExpirationException(String expiredToken) {
        super(expiredToken);
    }
}

package com.codehacks.blog.exception;

public class TokenExpirationException extends Throwable {

    public TokenExpirationException(String token_is_expired) {
        super(token_is_expired);
    }
}

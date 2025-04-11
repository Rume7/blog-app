package com.codehacks.blog.post.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidPostIdException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidPostIdException(String message) {
        super(message);
    }
}
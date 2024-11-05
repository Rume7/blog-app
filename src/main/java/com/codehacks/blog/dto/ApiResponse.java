package com.codehacks.blog.dto;

import java.time.LocalDateTime;

public class ApiResponse<T> {

    private T data;
    private String message;
    private LocalDateTime timestamp;
    private boolean success;

    public ApiResponse(T data, String message, boolean success) {
        this.data = data;
        this.message = message;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

}

package com.cliphub.common;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = "ok";
        response.data = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        response.data = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> fail(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.data = null;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> fail(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.data = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }
}

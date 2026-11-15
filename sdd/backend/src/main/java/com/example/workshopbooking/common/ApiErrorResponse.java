package com.example.workshopbooking.common;

import java.util.List;

public record ApiErrorResponse(
        int status,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public static ApiErrorResponse of(int status, String code, String message, String path) {
        return new ApiErrorResponse(status, code, message, path, List.of());
    }

    public record FieldError(String field, String code, String message) {
    }
}

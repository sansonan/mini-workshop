package com.example.workshopbooking.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::toFieldError)
                .toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "One or more fields are invalid.",
                request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), "MALFORMED_JSON", "The request body is malformed.",
                request.getRequestURI()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND", exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleForbidden(
            AccessDeniedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "You do not have permission to access this resource.",
                request.getRequestURI()));
    }

    private static ApiErrorResponse.FieldError toFieldError(FieldError error) {
        String code = switch (error.getCode() == null ? "" : error.getCode()) {
            case "NotBlank", "NotNull" -> "REQUIRED";
            case "Email" -> "INVALID_EMAIL";
            case "Size" -> "TOO_LONG";
            default -> "INVALID_VALUE";
        };
        String message = error.getDefaultMessage() == null ? "Invalid value." : error.getDefaultMessage();
        return new ApiErrorResponse.FieldError(error.getField(), code, message);
    }
}

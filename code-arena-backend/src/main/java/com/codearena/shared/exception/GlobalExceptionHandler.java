package com.codearena.shared.exception;

import com.codearena.shared.response.ApiResponse;
import com.codearena.shared.response.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles custom API exceptions.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        // TODO: Add structured error codes.
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.<Void>builder()
            .success(false).message(ex.getMessage()).timestamp(Instant.now()).build());
    }

    /**
     * Handles bean validation exceptions.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        // TODO: Return field-level validation details.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<Void>builder()
            .success(false).message("Validation failed").timestamp(Instant.now()).build());
    }

    /**
     * Handles authorization exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        // TODO: Add audit logging for forbidden access.
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.<Void>builder()
            .success(false).message("Access denied").timestamp(Instant.now()).build());
    }
    /*for validation errors shop*/

}

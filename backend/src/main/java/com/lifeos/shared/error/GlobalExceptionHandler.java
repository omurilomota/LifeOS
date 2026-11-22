package com.lifeos.shared.error;

import com.lifeos.shared.time.TimeProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final TimeProvider timeProvider;

    public GlobalExceptionHandler(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), List.of(), request, ex, false);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.ApiErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", details, request, ex, false);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.ApiErrorDetail> details = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.ApiErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", details, request, ex, false);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password", List.of(), request, ex, false);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied", List.of(), request, ex, false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", List.of(), request, ex, true);
    }

    private ApiError.ApiErrorDetail toDetail(FieldError error) {
        return new ApiError.ApiErrorDetail(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           List<ApiError.ApiErrorDetail> details,
                                           HttpServletRequest request, Exception ex, boolean logStack) {
        String traceId = UUID.randomUUID().toString();
        if (logStack) {
            log.error("traceId={} path={} code={}", traceId, request.getRequestURI(), code, ex);
        } else {
            log.warn("traceId={} path={} code={} message={}", traceId, request.getRequestURI(), code, ex.getMessage());
        }
        ApiError body = new ApiError(code, message, details, traceId, timeProvider.now());
        return ResponseEntity.status(status).body(body);
    }
}

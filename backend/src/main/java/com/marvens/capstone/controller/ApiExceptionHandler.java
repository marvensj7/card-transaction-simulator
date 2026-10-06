package com.marvens.capstone.controller;

import com.marvens.capstone.controller.dto.ApiError;
import com.marvens.capstone.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerMapping;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidPurchaseException.class)
    public ResponseEntity<ApiError> invalidPurchase(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PURCHASE",
                "Check the amount, merchant name, and assigned fictional card details.", request);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> invalidRequest(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Use a UUID request ID, a nonnegative page, and a positive page size.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> wrongRole(HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "This operation is not available for this user role.", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> unavailable(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "The requested resource is unavailable.", request);
    }

    @ExceptionHandler(RequestConflictException.class)
    public ResponseEntity<ApiError> conflictingRetry(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "REQUEST_CONFLICT",
                "Request ID is already used for different transaction details.", request);
    }

    @ExceptionHandler(RefundNotEligibleException.class)
    public ResponseEntity<ApiError> ineligibleRefund(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "REFUND_NOT_ELIGIBLE", "This purchase is not eligible for a full refund.", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        // Use the server's route template, never the raw URI, query, or exception message.
        log.info("API rejection: status={} code={} route={} user={}", status.value(), code,
                route(request), user(request));
        return ResponseEntity.status(status).body(ApiError.of(status, code, message));
    }

    private String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? "unmapped" : pattern.toString();
    }

    private String user(HttpServletRequest request) {
        return request.getUserPrincipal() instanceof AuthenticatedUser user ? user.getName() : "anonymous";
    }
}

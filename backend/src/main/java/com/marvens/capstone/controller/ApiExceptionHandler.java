package com.marvens.capstone.controller;

import com.marvens.capstone.controller.dto.ApiError;
import com.marvens.capstone.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.validation.method.ParameterErrors;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<ApiError> authenticationRequired(HttpServletRequest request) {
        ApiError error = ApiError.authenticationRequired();
        return error(HttpStatus.UNAUTHORIZED, error.code(), error.message(), request);
    }

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

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest webRequest) {
        var request = ((ServletWebRequest) webRequest).getRequest();
        String code = "INVALID_REQUEST";
        String message = "Check the request fields and types.";
        if (exception instanceof HttpMessageNotReadableException) {
            code = "MALFORMED_JSON";
            message = "Request body must contain valid JSON with the expected field types.";
        } else if (exception instanceof MethodArgumentNotValidException invalid) {
            code = "VALIDATION_FAILED";
            message = invalid.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField()).distinct().sorted()
                    .map(this::fieldMessage).collect(Collectors.joining(" "));
            if (message.isEmpty()) message = "Check the required request fields.";
        } else if (exception instanceof HandlerMethodValidationException invalid) {
            // MVC can combine @Valid body errors with direct path/query constraints.
            code = invalid.getParameterValidationResults().stream().anyMatch(result -> result instanceof ParameterErrors)
                    ? "VALIDATION_FAILED" : "INVALID_PARAMETER";
            message = invalid.getParameterValidationResults().stream()
                    .flatMap(result -> result instanceof ParameterErrors errors
                            ? errors.getFieldErrors().stream().map(error -> error.getField())
                            : Stream.of(result.getMethodParameter().getParameterName()))
                    .distinct().sorted()
                    .map(this::fieldMessage).collect(Collectors.joining(" "));
        } else if (exception instanceof TypeMismatchException) {
            code = "INVALID_PARAMETER";
            message = exception instanceof MethodArgumentTypeMismatchException invalid
                    ? fieldMessage(invalid.getName()) : "Use the expected parameter types.";
        }
        log.info("API rejection: status={} code={} route={} user={}", status.value(), code, route(request), user(request));
        // Preserve Spring's status and headers, but replace its body and exception detail.
        return new ResponseEntity<>(ApiError.of(status, code, message), headers, status);
    }

    private String fieldMessage(String field) {
        if (field == null) return "Check the request parameters.";
        return switch (field) {
            case "cardId", "accountId", "purchaseId" -> "Resource IDs must be positive integers.";
            case "testCardNumber" -> "Enter a 16-digit fictional test card number.";
            case "testSecurityCode" -> "Test security code must contain three or four digits.";
            case "expiryMonth" -> "Expiry month must be an integer from 1 to 12.";
            case "expiryYear" -> "Expiry year must be an integer from 2000 to 9999.";
            case "merchantName" -> "Merchant name is required and must be at most 100 characters.";
            case "amount" -> "Amount must be a positive decimal string with at most 12 whole digits and two decimal places.";
            case "requestId" -> "Request ID must be a UUID.";
            case "status" -> "Account status must be ACTIVE or FROZEN.";
            case "page" -> "Page must be a nonnegative integer.";
            case "size" -> "Page size must be a positive integer.";
            default -> "Check the request fields.";
        };
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

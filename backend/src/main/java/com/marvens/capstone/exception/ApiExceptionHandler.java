package com.marvens.capstone.exception;

import java.util.Arrays;
import com.marvens.capstone.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** One place translates service and Spring MVC failures into safe HTTP responses. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<ApiError> authenticationRequired() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.authenticationRequired());
    }

    @ExceptionHandler(InvalidPurchaseException.class)
    public ResponseEntity<ApiError> invalidPurchase() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PURCHASE",
                "Check the amount, merchant name, and assigned fictional card details.");
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> invalidRequest() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Use a UUID request ID, a nonnegative page, and a positive page size.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> wrongRole() {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "This operation is not available for this user role.");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> unavailable() {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "The requested resource is unavailable.");
    }

    @ExceptionHandler(RequestConflictException.class)
    public ResponseEntity<ApiError> conflictingRetry() {
        return error(HttpStatus.CONFLICT, "REQUEST_CONFLICT", "Request ID is already used for different transaction details.");
    }

    @ExceptionHandler(RefundNotEligibleException.class)
    public ResponseEntity<ApiError> ineligibleRefund() {
        return error(HttpStatus.CONFLICT, "REFUND_NOT_ELIGIBLE", "This purchase is not eligible for a full refund.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception failure) {
        return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(internalError(failure));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ApiError error;
        if (status.is5xxServerError()) {
            error = internalError(exception);
        } else if (exception instanceof HttpMessageNotReadableException) {
            error = ApiError.of(status, "MALFORMED_JSON", "Request body must contain valid JSON with the expected field types.");
        } else if (exception instanceof MethodArgumentNotValidException) {
            error = ApiError.of(status, "VALIDATION_FAILED", "Check the required fields and allowed values.");
        } else if (exception instanceof HandlerMethodValidationException invalid) {
            boolean bodyError = invalid.getParameterValidationResults().stream()
                    .anyMatch(result -> result instanceof ParameterErrors);
            error = ApiError.of(status, bodyError ? "VALIDATION_FAILED" : "INVALID_PARAMETER",
                    "Check the required fields, parameter types, and allowed values.");
        } else if (exception instanceof TypeMismatchException || exception instanceof MissingServletRequestParameterException) {
            error = ApiError.of(status, "INVALID_PARAMETER", "Check the parameter types and allowed values.");
        } else {
            String code = switch (status.value()) {
                case 404 -> "RESOURCE_NOT_FOUND";
                case 405 -> "METHOD_NOT_ALLOWED";
                case 406 -> "NOT_ACCEPTABLE";
                case 415 -> "UNSUPPORTED_MEDIA_TYPE";
                default -> "INVALID_REQUEST";
            };
            error = ApiError.of(status, code, "Check the route, HTTP method, required parameters, and JSON content type.");
        }
        // Keep Spring's status and headers (including Allow), and replace internal details.
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.putAll(headers);
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(error, jsonHeaders, status);
    }

    private ApiError internalError(Exception failure) {
        // Messages and causes may contain input. Log only the type and source locations.
        log.error("API failure: status=500 type={} frames={}", failure.getClass().getName(),
                Arrays.toString(Arrays.copyOf(failure.getStackTrace(), Math.min(12, failure.getStackTrace().length))));
        return ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(ApiError.of(status, code, message));
    }
}

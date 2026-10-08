package com.marvens.capstone.exception;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception failure, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = "Check the request path, fields, parameters, and JSON content type.";
        if (status.is5xxServerError()) {
            message = "An unexpected error occurred.";
            log.error("API failure type={}", failure.getClass().getName());
        } else if (failure instanceof ResponseStatusException) {
            // Our services supply fixed messages, never rejected values or database details.
            ResponseStatusException expected = (ResponseStatusException) failure;
            message = expected.getReason();
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(Map.of("message", message), responseHeaders, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception failure) {
        // Logging the exception itself would also print its possibly sensitive message.
        log.error("API failure type={}", failure.getClass().getName());
        return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", "An unexpected error occurred."));
    }
}

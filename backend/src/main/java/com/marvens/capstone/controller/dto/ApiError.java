package com.marvens.capstone.controller.dto;

import java.time.Instant;
import org.springframework.http.HttpStatusCode;

public record ApiError(int status, String code, String message, String timestamp) {
    public static ApiError of(HttpStatusCode status, String code, String message) {
        return new ApiError(status.value(), code, message, Instant.now().toString());
    }

    public static ApiError authenticationRequired() {
        return of(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "Authentication is required.");
    }
}

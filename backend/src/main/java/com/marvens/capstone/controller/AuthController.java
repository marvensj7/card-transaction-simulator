package com.marvens.capstone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import com.marvens.capstone.dto.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.marvens.capstone.dto.UserResponse;
import com.marvens.capstone.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @Operation(summary = "Register a USER with a credit account and fictional card")
    @ApiResponse(responseCode = "201", description = "Customer, account, and fictional card created together")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.created(URI.create("/api/auth/me")).body(auth.register(request));
    }
    @Operation(summary = "Check BCrypt credentials and issue a 15-minute access token")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }

    @Operation(summary = "Read the current user")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt principal) {
        return auth.currentUser(Long.valueOf(principal.getSubject()));
    }
}

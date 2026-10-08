package com.marvens.capstone.controller;

import java.net.URI;
import com.marvens.capstone.dto.RegisterRequest;
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

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.created(URI.create("/api/auth/me")).body(auth.register(request));
    }
}

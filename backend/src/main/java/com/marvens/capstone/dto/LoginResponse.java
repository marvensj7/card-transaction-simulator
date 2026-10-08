package com.marvens.capstone.dto;

import java.time.Instant;
import com.marvens.capstone.entity.AppUser;
import org.springframework.security.oauth2.jwt.Jwt;

public class LoginResponse {
    public final AppUser user;
    public final String accessToken;
    public final Instant expiresAt;
    public final String tokenType;

    public LoginResponse(AppUser user, Jwt token) {
        tokenType = "Bearer";
        this.user = user;
        accessToken = token.getTokenValue();
        expiresAt = token.getExpiresAt();
    }

    @Override
    public String toString() { return "LoginResponse"; }
}

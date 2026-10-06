package com.marvens.capstone.controller;

import java.security.Principal;

/** Identity established by server-side authentication, never by request fields. */
public record AuthenticatedUser(Long userId) implements Principal {
    public AuthenticatedUser {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("Authenticated user ID must be positive.");
        }
    }

    @Override
    public String getName() { return userId.toString(); }
}

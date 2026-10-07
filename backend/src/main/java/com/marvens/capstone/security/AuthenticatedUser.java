package com.marvens.capstone.security;

import java.security.Principal;
import com.marvens.capstone.exception.AuthenticationRequiredException;

/** Identity established by server-side authentication, never by request fields. */
public record AuthenticatedUser(Long userId) implements Principal {
    public AuthenticatedUser {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("Authenticated user ID must be positive.");
        }
    }

    public static Long id(Principal principal) {
        if (principal instanceof AuthenticatedUser user) return user.userId();
        throw new AuthenticationRequiredException();
    }

    @Override
    public String getName() { return userId.toString(); }
}

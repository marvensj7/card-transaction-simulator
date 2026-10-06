package com.marvens.capstone.controller;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class CurrentUser {
    private CurrentUser() { }

    public static Long id(Principal principal) {
        if (principal instanceof AuthenticatedUser user) {
            return user.userId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}

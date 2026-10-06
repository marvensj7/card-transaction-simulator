package com.marvens.capstone.controller;

import java.security.Principal;
import com.marvens.capstone.exception.AuthenticationRequiredException;

public final class CurrentUser {
    private CurrentUser() { }

    public static Long id(Principal principal) {
        if (principal instanceof AuthenticatedUser user) {
            return user.userId();
        }
        throw new AuthenticationRequiredException();
    }
}

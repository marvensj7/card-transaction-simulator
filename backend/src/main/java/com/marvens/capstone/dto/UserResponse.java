package com.marvens.capstone.dto;

import com.marvens.capstone.entity.AppUser;

public class UserResponse {
    public final Long id;
    public final String displayName;
    public final String email;
    public final AppUser.Role role;

    public UserResponse(AppUser user) {
        id = user.getId();
        displayName = user.getDisplayName();
        email = user.getEmail();
        role = user.getRole();
    }
}

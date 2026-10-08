package com.marvens.capstone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public class LoginRequest {
    @NotBlank @Email @Size(max = 150)
    public String email;
    @NotBlank @Size(max = 72)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String password;

    @Override
    public String toString() { return "LoginRequest"; }
}

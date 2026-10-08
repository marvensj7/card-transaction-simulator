package com.marvens.capstone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public class RegisterRequest {
    @NotBlank @Size(max = 100)
    public String displayName;
    @NotBlank @Email @Size(max = 150)
    public String email;
    @NotBlank @Size(min = 12, max = 72)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String password;

    @Override
    public String toString() { return "RegisterRequest"; }
}

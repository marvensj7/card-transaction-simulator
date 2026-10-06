package com.marvens.capstone.exception;

public class AuthenticationRequiredException extends RuntimeException {
    public AuthenticationRequiredException() { super("Authentication is required."); }
}

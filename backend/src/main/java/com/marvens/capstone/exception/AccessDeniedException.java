package com.marvens.capstone.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException() { super("This operation is not available for this user role."); }
}

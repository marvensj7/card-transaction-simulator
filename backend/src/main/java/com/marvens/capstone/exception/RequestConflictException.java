package com.marvens.capstone.exception;

public class RequestConflictException extends RuntimeException {
    public RequestConflictException() { super("Request ID is already used for different transaction details."); }
}

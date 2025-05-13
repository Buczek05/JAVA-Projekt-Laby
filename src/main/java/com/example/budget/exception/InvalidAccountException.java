package com.example.budget.exception;

public class InvalidAccountException extends RuntimeException {
    
    public InvalidAccountException(String message) {
        super(message);
    }
    
    public InvalidAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
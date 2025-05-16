package com.example.budget.exception;

public class InvalidTransactionException extends RuntimeException {
    
    public InvalidTransactionException(String message) {
        super(message);
    }
    
    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
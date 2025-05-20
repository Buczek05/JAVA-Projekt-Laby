package com.example.budget.exception;

/**
 * Exception thrown when a user is invalid.
 */
public class InvalidUserException extends RuntimeException {
    public InvalidUserException(String message) {
        super(message);
    }
}
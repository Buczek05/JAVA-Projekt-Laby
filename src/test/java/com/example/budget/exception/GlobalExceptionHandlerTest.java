package com.example.budget.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exceptionHandler = new GlobalExceptionHandler();
        objectMapper = new ObjectMapper();
        
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleAccountNotFoundException_ReturnsCorrectErrorResponse() {
        // Given
        String errorMessage = "Account not found with id: 999";
        AccountNotFoundException exception = new AccountNotFoundException(errorMessage);

        // When
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleAccountNotFoundException(exception, request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        
        ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
        assertEquals("Account Not Found", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
        assertEquals("/api/test", errorResponse.getPath());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void handleInsufficientFundsException_ReturnsCorrectErrorResponse() {
        // Given
        String errorMessage = "Insufficient funds in account: 123";
        InsufficientFundsException exception = new InsufficientFundsException(errorMessage);

        // When
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleInsufficientFundsException(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        
        ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
        assertEquals("Insufficient Funds", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
        assertEquals("/api/test", errorResponse.getPath());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void handleGenericException_ReturnsCorrectErrorResponse() {
        // Given
        String errorMessage = "Unexpected error occurred";
        Exception exception = new Exception(errorMessage);

        // When
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleGenericException(exception, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        
        ErrorResponse errorResponse = responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.getStatus());
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals(errorMessage, errorResponse.getMessage());
        assertEquals("/api/test", errorResponse.getPath());
        assertNotNull(errorResponse.getTimestamp());
    }
}
package com.example.budget.controller;

import com.example.budget.controller.dto.AuthRequest;
import com.example.budget.controller.dto.AuthResponse;
import com.example.budget.controller.dto.RegisterRequest;
import com.example.budget.entity.Role;
import com.example.budget.entity.User;
import com.example.budget.exception.InvalidUserException;
import com.example.budget.security.JwtTokenProvider;
import com.example.budget.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthController authController;

    @ControllerAdvice
    static class TestControllerAdvice {

        @ExceptionHandler(InvalidUserException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleInvalidUserException(InvalidUserException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(BadCredentialsException.class)
        @ResponseStatus(HttpStatus.UNAUTHORIZED)
        public ResponseEntity<String> handleBadCredentialsException(BadCredentialsException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new TestControllerAdvice())
                .build();
    }

    @Test
    void registerUser_ValidRequest_ReturnsCreatedUserWithToken() throws Exception {
        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        User createdUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                createdUser, null, createdUser.getAuthorities());

        when(userService.createUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getRole()
        )).thenReturn(createdUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token");

        // When
        ResultActions result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is("jwt-token")))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    void registerUser_DuplicateUsername_ReturnsBadRequest() throws Exception {
        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("existinguser")
                .email("test@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        when(userService.createUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getRole()
        )).thenThrow(new InvalidUserException("Username already exists: existinguser"));

        // When
        ResultActions result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_DuplicateEmail_ReturnsBadRequest() throws Exception {
        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("existing@example.com")
                .password("password123")
                .role(Role.USER)
                .build();

        when(userService.createUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getRole()
        )).thenThrow(new InvalidUserException("Email already exists: existing@example.com"));

        // When
        ResultActions result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    void registerUser_InvalidInput_ReturnsBadRequest() throws Exception {
        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("")  // Invalid: empty username
                .email("invalid-email")  // Invalid: not an email
                .password("123")  // Invalid: too short
                .role(Role.USER)
                .build();

        // When
        ResultActions result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    void authenticateUser_ValidCredentials_ReturnsTokenAndUserInfo() throws Exception {
        // Given
        AuthRequest authRequest = AuthRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token");

        when(userService.getUserByUsername("testuser")).thenReturn(user);

        // When
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is("jwt-token")))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    void authenticateUser_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Given
        AuthRequest authRequest = AuthRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        // When
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)));

        // Then
        result.andExpect(status().isUnauthorized());
    }
}

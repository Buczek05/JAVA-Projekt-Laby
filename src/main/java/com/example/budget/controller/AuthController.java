package com.example.budget.controller;

import com.example.budget.controller.dto.AuthRequest;
import com.example.budget.controller.dto.AuthResponse;
import com.example.budget.controller.dto.RegisterRequest;
import com.example.budget.entity.Role;
import com.example.budget.entity.User;
import com.example.budget.security.JwtTokenProvider;
import com.example.budget.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Authenticate a user and generate a JWT token.
     *
     * @param authRequest the authentication request
     * @return the authentication response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
        
        User user = userService.getUserByUsername(authRequest.getUsername());
        
        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwt)
                .username(user.getUsername())
                .role(user.getRole())
                .build());
    }

    /**
     * Register a new user.
     *
     * @param registerRequest the registration request
     * @return the authentication response with JWT token
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        // Default to USER role if not specified
        Role role = registerRequest.getRole() != null ? registerRequest.getRole() : Role.USER;
        
        User user = userService.createUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                role
        );
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getUsername(),
                        registerRequest.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
        
        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwt)
                .username(user.getUsername())
                .role(user.getRole())
                .build());
    }
}
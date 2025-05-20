package com.example.budget.service;

import com.example.budget.entity.Role;
import com.example.budget.entity.User;
import com.example.budget.exception.InvalidUserException;
import com.example.budget.exception.UserNotFoundException;
import com.example.budget.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private String validUsername;
    private String validEmail;
    private String validPassword;
    private String encodedPassword;
    private Role validRole;
    private User savedUser;

    @BeforeEach
    void setUp() {
        validUsername = "testuser";
        validEmail = "test@example.com";
        validPassword = "password123";
        encodedPassword = "encodedPassword123";
        validRole = Role.USER;

        savedUser = User.builder()
                .id(1L)
                .username(validUsername)
                .email(validEmail)
                .password(encodedPassword)
                .role(validRole)
                .build();
    }

    @Test
    void createUser_ValidUser_ReturnsCreatedUser() {
        // Given
        when(userRepository.existsByUsername(validUsername)).thenReturn(false);
        when(userRepository.existsByEmail(validEmail)).thenReturn(false);
        when(passwordEncoder.encode(validPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.createUser(validUsername, validEmail, validPassword, validRole);

        // Then
        assertNotNull(result);
        assertEquals(savedUser.getId(), result.getId());
        assertEquals(validUsername, result.getUsername());
        assertEquals(validEmail, result.getEmail());
        assertEquals(encodedPassword, result.getPassword());
        assertEquals(validRole, result.getRole());
        
        verify(userRepository).existsByUsername(validUsername);
        verify(userRepository).existsByEmail(validEmail);
        verify(passwordEncoder).encode(validPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_DuplicateUsername_ThrowsInvalidUserException() {
        // Given
        when(userRepository.existsByUsername(validUsername)).thenReturn(true);

        // When & Then
        InvalidUserException exception = assertThrows(InvalidUserException.class, 
            () -> userService.createUser(validUsername, validEmail, validPassword, validRole));
        
        assertEquals("Username already exists: " + validUsername, exception.getMessage());
        
        verify(userRepository).existsByUsername(validUsername);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_DuplicateEmail_ThrowsInvalidUserException() {
        // Given
        when(userRepository.existsByUsername(validUsername)).thenReturn(false);
        when(userRepository.existsByEmail(validEmail)).thenReturn(true);

        // When & Then
        InvalidUserException exception = assertThrows(InvalidUserException.class, 
            () -> userService.createUser(validUsername, validEmail, validPassword, validRole));
        
        assertEquals("Email already exists: " + validEmail, exception.getMessage());
        
        verify(userRepository).existsByUsername(validUsername);
        verify(userRepository).existsByEmail(validEmail);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserByUsername_ExistingUsername_ReturnsUser() {
        // Given
        when(userRepository.findByUsername(validUsername)).thenReturn(Optional.of(savedUser));

        // When
        User result = userService.getUserByUsername(validUsername);

        // Then
        assertNotNull(result);
        assertEquals(savedUser.getId(), result.getId());
        assertEquals(validUsername, result.getUsername());
        assertEquals(validEmail, result.getEmail());
        assertEquals(encodedPassword, result.getPassword());
        assertEquals(validRole, result.getRole());
        
        verify(userRepository).findByUsername(validUsername);
    }

    @Test
    void getUserByUsername_NonExistingUsername_ThrowsUserNotFoundException() {
        // Given
        String nonExistingUsername = "nonexistinguser";
        when(userRepository.findByUsername(nonExistingUsername)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, 
            () -> userService.getUserByUsername(nonExistingUsername));
        
        assertEquals("User not found: " + nonExistingUsername, exception.getMessage());
        
        verify(userRepository).findByUsername(nonExistingUsername);
    }

    @Test
    void loadUserByUsername_ExistingUsername_ReturnsUserDetails() {
        // Given
        when(userRepository.findByUsername(validUsername)).thenReturn(Optional.of(savedUser));

        // When
        UserDetails result = userService.loadUserByUsername(validUsername);

        // Then
        assertNotNull(result);
        assertEquals(validUsername, result.getUsername());
        assertEquals(encodedPassword, result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        
        verify(userRepository).findByUsername(validUsername);
    }

    @Test
    void loadUserByUsername_NonExistingUsername_ThrowsUsernameNotFoundException() {
        // Given
        String nonExistingUsername = "nonexistinguser";
        when(userRepository.findByUsername(nonExistingUsername)).thenReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, 
            () -> userService.loadUserByUsername(nonExistingUsername));
        
        assertEquals("User not found: " + nonExistingUsername, exception.getMessage());
        
        verify(userRepository).findByUsername(nonExistingUsername);
    }
}
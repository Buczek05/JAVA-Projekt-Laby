package com.example.budget.service;

import com.example.budget.entity.Role;
import com.example.budget.entity.User;
import com.example.budget.exception.InvalidUserException;
import com.example.budget.exception.UserNotFoundException;
import com.example.budget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for managing users.
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user.
     *
     * @param username the username
     * @param email the email
     * @param password the password
     * @param role the role
     * @return the created user
     * @throws InvalidUserException if the username or email already exists
     */
    public User createUser(String username, String email, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new InvalidUserException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new InvalidUserException("Email already exists: " + email);
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();

        return userRepository.save(user);
    }

    /**
     * Get a user by username.
     *
     * @param username the username
     * @return the user
     * @throws UserNotFoundException if the user is not found
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
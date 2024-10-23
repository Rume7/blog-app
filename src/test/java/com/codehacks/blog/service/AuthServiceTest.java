package com.codehacks.blog.service;

import com.codehacks.blog.model.User;
import com.codehacks.blog.repository.UserRepository;
import com.codehacks.blog.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setUsername("testUser");
        user.setPassword("encodedPassword");
    }

    @Test
    void testAuthenticateSuccess() {
        // Given
        String username = "testUser";
        String password = "password";
        String encodedPassword = new BCryptPasswordEncoder().encode(password);
        user.setPassword(encodedPassword);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(username)).thenReturn("testToken");

        // When
        String result = authService.authenticate("testUser", "password");

        // Then
        assertEquals("testToken", result);
        verify(userRepository, times(1)).findByUsername(username);
        verify(jwtUtil, times(1)).generateToken(username);
    }

    @Test
    void testAuthenticateUserNotFound() {
        // Given
        when(userRepository.findByUsername("invalidUser")).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> authService.authenticate("invalidUser", "password"));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testAuthenticateInvalidPassword() {
        // Given
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        //when(passwordEncoder.matches("wrongPassword", user.getPassword())).thenReturn(false);

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> authService.authenticate("testUser", "wrongPassword"));
        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    void testRegister() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(user);
        user.setPassword("password"); // Set plain password to encode

        // When
        User result = authService.register(user);

        // Then
        assertNotNull(result);
        assertNotEquals("password", result.getPassword()); // Check password is encoded
        verify(userRepository).save(any(User.class));
    }
}

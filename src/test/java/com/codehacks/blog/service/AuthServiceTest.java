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
        String result = authService.authenticate(username, password);

        // Then
        assertEquals("testToken", result);
        verify(userRepository, times(1)).findByUsername(username);
        verify(jwtUtil, times(1)).generateToken(username);
    }

    @Test
    void testAuthenticateUserNotFound() {
        // Given & When
        when(userRepository.findByUsername("invalidUser")).thenReturn(Optional.empty());

        // Then
        Exception exception = assertThrows(RuntimeException.class,
                () -> authService.authenticate("invalidUser", "password"));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testAuthenticateInvalidPassword() {
        // Given & When
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        // Then
        Exception exception = assertThrows(RuntimeException.class,
                () -> authService.authenticate("testUser", "wrongPassword"));
        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    void testRegister() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(user);
        String encodedPassword = new BCryptPasswordEncoder().encode("password");
        user.setPassword(encodedPassword);

        // When
        User result = authService.register(user);

        // Then
        assertNotNull(result);
        assertNotEquals("password", result.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testChangePasswordSuccess() {
        // Given
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setUsername("testUser");
        String currentPassword = "currentPassword";
        String encodedCurrentPassword = passwordEncoder.encode(currentPassword);
        user.setPassword(encodedCurrentPassword);

        // When
        when(userRepository.findByUsername("testUser")).thenReturn(java.util.Optional.of(user));

        authService.changePassword("testUser", currentPassword, "newPassword");

        // Then
        assertNotEquals(encodedCurrentPassword, user.getPassword());
        assertTrue(passwordEncoder.matches("newPassword", user.getPassword()));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testChangePasswordUserNotFound() {
        // Given & When
        when(userRepository.findByUsername("testUser")).thenReturn(java.util.Optional.empty());

        // Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.changePassword("testUser", "anyPassword", "newPassword"));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testChangePasswordInvalidCurrentPassword() {
        // Given & When
        when(userRepository.findByUsername("testUser")).thenReturn(java.util.Optional.of(user));
        String wrongCurrentPassword = "wrongEncodedPassword";

        // Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.changePassword("testUser", wrongCurrentPassword, "newPassword"));

        assertEquals("Invalid current password", exception.getMessage());
    }

    @Test
    void testDeleteAccountSuccess() {
        // Given
        when(userRepository.findByUsername("testUser")).thenReturn(java.util.Optional.of(user));

        // When
        authService.deleteAccount("testUser");

        // Then
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void testDeleteAccountUserNotFound() {
        // Given & When
        when(userRepository.findByUsername("testUser")).thenReturn(java.util.Optional.empty());

        // Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.deleteAccount("testUser"));
        assertEquals("User not found", exception.getMessage());
    }
}

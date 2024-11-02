package com.codehacks.blog.service;

import com.codehacks.blog.exception.UserAccountNotFound;
import com.codehacks.blog.model.User;
import com.codehacks.blog.repository.UserRepository;
import com.codehacks.blog.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        // Encode password correctly
        String password = "password";
        user.setPassword(passwordEncoder.encode(password));
    }

    @Test
    void testAuthenticateSuccess() {
        // Given
        String password = "password";

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getUsername())).thenReturn("testToken");

        // When
        String result = authService.authenticate("testUser", password);

        // Then
        assertEquals("testToken", result);
        verify(userRepository, times(1)).findByUsername("testUser");
        verify(jwtUtil, times(1)).generateToken(user.getUsername());
    }

    @Test
    void testAuthenticateUserNotFound() {
        // Given & When
        when(userRepository.findByUsername("nonExistentUser")).thenReturn(Optional.empty());

        // Then
        assertThrows(UserAccountNotFound.class,
                () -> authService.authenticate("nonExistentUser", "password"));
    }

    @Test
    void testAuthenticateInvalidPassword() {
        // Given & When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // Then
        UserAccountNotFound exception = assertThrows(UserAccountNotFound.class,
                () -> authService.authenticate(user.getUsername(), "wrongPassword")); // Wrong password
        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    void testRegister() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(user);
        user.setPassword("password"); // Set plain password for registration

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
        String currentPassword = "password";
        user.setPassword(passwordEncoder.encode(currentPassword));

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // When
        authService.changePassword(user.getUsername(), currentPassword, "newPassword");

        // Then
        assertTrue(passwordEncoder.matches("newPassword", user.getPassword()));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testChangePasswordUserNotFound() {
        // Given & When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());

        // Then
        UserAccountNotFound exception = assertThrows(UserAccountNotFound.class,
                () -> authService.changePassword(user.getUsername(), "anyPassword", "newPassword"));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testChangePasswordInvalidCurrentPassword() {
        // Given & When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // Then
        UserAccountNotFound exception = assertThrows(UserAccountNotFound.class,
                () -> authService.changePassword(user.getUsername(), "wrongPassword", "newPassword"));

        assertEquals("Invalid current password", exception.getMessage());
    }

    @Test
    void testDeleteAccountSuccess() {
        // Given
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // When
        authService.deleteAccount(user.getUsername());

        // Then
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void testDeleteAccountUserNotFound() {
        // Given & When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());

        // Then
        UserAccountNotFound exception = assertThrows(UserAccountNotFound.class,
                () -> authService.deleteAccount(user.getUsername()));
        assertEquals("User account not found", exception.getMessage());
    }
}

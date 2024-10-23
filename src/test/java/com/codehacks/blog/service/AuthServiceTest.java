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
        String password = "password";
        String encodedPassword = new BCryptPasswordEncoder().encode(password);
        user.setPassword(encodedPassword);

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getUsername())).thenReturn("testToken");

        // When
        String result = authService.authenticate(user.getUsername(), password);

        // Then
        assertEquals("testToken", result);
        verify(userRepository, times(1)).findByUsername(user.getUsername());
        verify(jwtUtil, times(1)).generateToken(user.getUsername());
    }

    @Test
    void testAuthenticateUserNotFound() {
        // Given & When
        when(userRepository.findByUsername("invalidUser")).thenReturn(Optional.empty());

        // Then
        RuntimeException exception = assertThrows(UserAccountNotFound.class,
                () -> authService.authenticate("invalidUser", "password"));
        assertEquals("Invalid login credentials", exception.getMessage());
    }

    @Test
    void testAuthenticateInvalidPassword() {
        // Given & When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // Then
        RuntimeException exception = assertThrows(UserAccountNotFound.class,
                () -> authService.authenticate(user.getUsername(), "wrongPassword"));
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
    public void testChangePasswordSuccess() {
        // Given
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        String currentPassword = "currentPassword";
        String encodedCurrentPassword = passwordEncoder.encode(currentPassword);
        user.setPassword(encodedCurrentPassword);

        // When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(java.util.Optional.of(user));

        authService.changePassword(user.getUsername(), currentPassword, "newPassword");

        // Then
        assertNotEquals(encodedCurrentPassword, user.getPassword());
        assertTrue(passwordEncoder.matches("newPassword", user.getPassword()));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    public void testChangePasswordUserNotFound() {
        // Given & When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(java.util.Optional.empty());

        // Then
        RuntimeException exception = assertThrows(UserAccountNotFound.class,
                () -> authService.changePassword(user.getUsername(), "anyPassword", "newPassword"));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    public void testChangePasswordInvalidCurrentPassword() {
        // Given
        String wrongCurrentPassword = "wrongEncodedPassword";

        // When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(java.util.Optional.of(user));

        // Then
        RuntimeException exception = assertThrows(UserAccountNotFound.class,
                () -> authService.changePassword(user.getUsername(), wrongCurrentPassword, "newPassword"));

        assertEquals("Invalid current password", exception.getMessage());
    }

    @Test
    public void testDeleteAccountSuccess() {
        // Given & When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(java.util.Optional.of(user));
        authService.deleteAccount(user.getUsername());

        // Then
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    public void testDeleteAccountUserNotFound() {
        // Given & When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(java.util.Optional.empty());

        // Then
        RuntimeException exception = assertThrows(UserAccountNotFound.class,
                () -> authService.deleteAccount(user.getUsername()));
        assertEquals("User account not found", exception.getMessage());
    }
}

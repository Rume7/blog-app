package com.codehacks.blog.service;

import com.codehacks.blog.auth.model.CustomUserDetails;
import com.codehacks.blog.auth.service.AdminService;
import com.codehacks.blog.auth.service.AuthService;
import com.codehacks.blog.auth.service.TokenService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codehacks.blog.auth.exception.UserAccountException;
import com.codehacks.blog.auth.mapper.UserMapper;
import com.codehacks.blog.auth.model.Role;
import com.codehacks.blog.auth.model.User;
import com.codehacks.blog.auth.repository.UserRepository;
import com.codehacks.blog.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetails userDetails;

    @Mock
    private AdminService adminService;

    @Mock
    private UserMapper userMapper;

    private User user;

    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("password"));
    }

    @Test
    void testAuthenticateWhenUserExists() {
        // Given
        String email = "user@example.com";
        Role role = Role.USER;
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        CustomUserDetails userDetails = new CustomUserDetails(
                "username", "encodedPassword", email, role, authorities, true
        );

        User user = new User(1L, "username", "encodedPassword", email, role, true, LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.findByUsername("username")).thenReturn(Optional.of(user));
        when(tokenService.generateToken(user)).thenReturn("generatedToken");

        // When
        String result = authService.authenticate(userDetails);

        // Then
        assertEquals("generatedToken", result);
        verify(userRepository, times(2)).findByUsername(user.getUsername());
        verify(tokenService, times(1)).generateToken(user);
    }


    @Test
    void testAuthenticateWhenUserNotFound() {
        // Given
        String email = "user@example.com";
        Role role = Role.USER;
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        CustomUserDetails userDetails = new CustomUserDetails(
                "username", "encodedPassword", email, role, authorities, true
        );

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // When & Then
        UserAccountException thrown = assertThrows(UserAccountException.class, () -> {
            authService.authenticate(userDetails);
        });

        assertEquals("Invalid login credentials", thrown.getMessage());
        verify(userRepository, never()).findByUsername(user.getUsername());
    }


    @Test
    void registerUser_whenUsernameExists_thenThrowException() {
        // Given
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // When & Then
        assertUserAccountException(() ->
                authService.registerUser(user, user.getPassword()), user.getUsername() + " already exist");
    }

    @Test
    void registerUser_EmailExists_ThrowsException() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(UserAccountException.class, () -> authService.registerUser(user, user.getPassword()));
    }

    @Test
    void logout_Success() {
        authService.logout(user.getEmail());
        verify(tokenService).invalidateToken(user.getEmail());
    }

    private void assertUserAccountException(Executable action, String expectedMessage) {
        UserAccountException exception = assertThrows(UserAccountException.class, action);
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void testChangePasswordUserNotFound() {
        // Given & When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());

        // Then
        assertUserAccountException(() -> authService.changePassword(
                user.getUsername(), "anyPassword", "newPassword"), "User not found");
    }

    @Test
    void changeUserRole_UserNotFound_ThrowsException() {
        //when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());

        assertThrows(UserAccountException.class, () ->
                authService.changeUserRole(user.getUsername(), Role.ADMIN)
        );
    }

    @Test
    void deleteUserAccount_whenUserExists_thenDeleteAccount() {
        // Given
        user.setRole(Role.ADMIN);
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // When
        authService.deleteUserAccount(user.getUsername());

        // Then
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void deleteUserAccount_whenUserNotFound_thenThrowException() {
        // Given
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());

        // When & Then
        assertUserAccountException(
                () -> authService.deleteUserAccount(user.getUsername()), "User account not found");
    }

    @Test
    void canUserDeleteAccount_whenUserIsAdmin_thenReturnTrue() {
        // Given
        user.setRole(Role.ADMIN);

        // When
        when(userDetails.getUsername()).thenReturn(user.getUsername());

        boolean canDelete = authService.canUserDeleteAccount(user.getUsername(), userDetails);

        // Then
        assertTrue(canDelete);
    }

    @Test
    void canUserDeleteAccount_whenUserIsNotAdmin_thenReturnTrueForSameUser() {
        // Given
        when(userDetails.getUsername()).thenReturn(user.getUsername());

        // When
        boolean canDelete = authService.canUserDeleteAccount(user.getUsername(), userDetails);

        // Then
        assertTrue(canDelete);
    }

    @Test
    void canUserDeleteAccount_whenDifferentUser_thenReturnFalse() {
        // Given
        when(userDetails.getUsername()).thenReturn("otherUsername");

        // When
        boolean canDelete = authService.canUserDeleteAccount(user.getUsername(), userDetails);

        // Then
        assertFalse(canDelete);
    }

    @Test
    void logAdminAccess_Success() {
        authService.logAdminAccess(user.getEmail(), "127.0.0.1");
        verify(adminService).logAdminAccess(user.getEmail(), "127.0.0.1");
    }

    @Test
    void reportUnauthorizedAdminAccess_Success() {
        authService.reportUnauthorizedAdminAccess(user.getEmail(), "127.0.0.1");
        verify(adminService).reportUnauthorizedAdminAccess(user.getEmail(), "127.0.0.1");
    }
}

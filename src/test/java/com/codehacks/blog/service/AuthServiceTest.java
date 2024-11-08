package com.codehacks.blog.service;

import com.codehacks.blog.dto.UserDTO;
import com.codehacks.blog.exception.UserAccountException;
import com.codehacks.blog.mapper.UserMapper;
import com.codehacks.blog.model.Role;
import com.codehacks.blog.model.User;
import com.codehacks.blog.repository.UserRepository;
import com.codehacks.blog.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetails userDetails;

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
        assertThrows(UserAccountException.class,
                () -> authService.authenticate("nonExistentUser", "password"));
    }

    @Test
    void testAuthenticateInvalidPassword() {
        // Given & When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // Then
        UserAccountException exception = assertThrows(UserAccountException.class,
                () -> authService.authenticate(user.getUsername(), "wrongPassword"));
        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    void authenticate_whenValidCredentials_thenReturnToken() {
        // Given
        String password = "password";

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getUsername())).thenReturn("validJwtToken");


        // When
        String token = authService.authenticate("testUser", password);

        // Then
        assertEquals("validJwtToken", token);
    }

    @Test
    void authenticate_whenInvalidUsername_thenThrowException() {
        // Given
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());

        // When
        UserAccountException exception = assertThrows(UserAccountException.class, () ->
                authService.authenticate(user.getUsername(), user.getPassword()));

        // Then
        assertEquals("Invalid login credentials", exception.getMessage());
    }

    @Test
    void registerUser_whenUserDoesNotExist_thenRegisterAndReturnDTO() {
        // Given
        user.setPassword("testPassword");

        // When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        UserDTO expectedUserDTO = new UserDTO(user.getEmail(), user.getUsername());
        when(userMapper.apply(any(User.class))).thenReturn(expectedUserDTO);

        User savedUser = new User();
        savedUser.setUsername(user.getUsername());
        savedUser.setEmail(user.getEmail());
        savedUser.setPassword("encodedPassword");
        savedUser.setRole(user.getRole());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDTO userDTO = authService.registerUser(user);

        // Then
        assertNotNull(userDTO);
        assertEquals(user.getUsername(), userDTO.getUsername());
        assertEquals(user.getEmail(), userDTO.getEmail());
    }

    @Test
    void registerUser_whenUsernameExists_thenThrowException() {
        // Given
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // When & Then
        UserAccountException exception = assertThrows(UserAccountException.class,
                () -> authService.registerUser(user));

        assertEquals(user.getUsername() + " already exist", exception.getMessage());
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
        UserAccountException exception = assertThrows(UserAccountException.class,
                () -> authService.changePassword(
                        user.getUsername(), "anyPassword", "newPassword"));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testChangePasswordInvalidCurrentPassword() {
        // Given & When
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // Then
        UserAccountException exception = assertThrows(UserAccountException.class,
                () -> authService.changePassword(
                        user.getUsername(), "wrongPassword", "newPassword"));

        assertEquals("Invalid current password", exception.getMessage());
    }

    @Test
    void changePassword_whenValidCurrentPassword_thenChangePassword() {
        // Given
        String currentPassword = "testPassword";
        String newPassword = "newPassword";

        String encodedCurrentPassword = new BCryptPasswordEncoder().encode(currentPassword);
        String encodedNewPassword = new BCryptPasswordEncoder().encode(newPassword);

        user.setPassword(encodedCurrentPassword);

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User updatedUser = invocation.getArgument(0);
            updatedUser.setPassword(encodedNewPassword);
            return updatedUser;
        });

        // When
        authService.changePassword(user.getUsername(), currentPassword, newPassword);

        // Then
        assertEquals(encodedNewPassword, user.getPassword());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void changeUserRole_whenValidRole_thenChangeRole() {
        // Given
        Role newRole = Role.SUBSCRIBER;

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        User updatedUser = new User();
        updatedUser.setUsername(user.getUsername());
        updatedUser.setEmail(user.getEmail());
        updatedUser.setPassword(user.getPassword());
        updatedUser.setRole(newRole);

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // When
        User result = authService.changeUserRole(user.getUsername(), newRole);

        // Then
        assertEquals(newRole, result.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void deleteUserAccount_whenUserExists_thenDeleteAccount() {
        // Given
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
        UserAccountException exception = assertThrows(UserAccountException.class,
                () -> authService.deleteUserAccount(user.getUsername()));

        assertEquals("User account not found", exception.getMessage());
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
}

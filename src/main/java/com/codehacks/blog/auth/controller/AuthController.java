package com.codehacks.blog.auth.controller;

import com.codehacks.blog.auth.dto.ApiResponse;
import com.codehacks.blog.auth.dto.AuthResponse;
import com.codehacks.blog.auth.dto.LoginRequest;
import com.codehacks.blog.auth.dto.PasswordChangeRequest;
import com.codehacks.blog.auth.dto.RegisterRequest;
import com.codehacks.blog.auth.dto.RoleChangeRequest;
import com.codehacks.blog.auth.dto.UserDTO;
import com.codehacks.blog.auth.model.CustomUserDetails;
import com.codehacks.blog.auth.model.User;
import com.codehacks.blog.auth.service.AuthService;
import com.codehacks.blog.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.AUTH_PATH)
@Validated
@Tag(name = "Authentication", description = "Authentication management API")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register new user")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<UserDTO>> register(@RequestBody @Valid RegisterRequest request) {
        UserDTO savedUser = authService.registerUser(request.toUser(), request.password());
        log.info("User registered with email: {}", savedUser.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "User registered successfully", savedUser));
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Login user")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Perform authentication using AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Extract authenticated user details
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

            String generateToken = authService.authenticate(customUserDetails);
            log.info("Token generated successfully for user: {}", customUserDetails.getUsername());

            // Set Authorization header
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + generateToken);

            // Build AuthResponse in both header and body.
            AuthResponse authResponse = new AuthResponse(generateToken, customUserDetails.getUsername(),
                    customUserDetails.getEmail(), customUserDetails.getRole().name());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ApiResponse<>(true, "Login successful", authResponse));

        } catch (AuthenticationException ex) {
            log.error("Login failed for email: {} - Error: {}", loginRequest.email(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Invalid email or password", null));
        }
    }

    @PutMapping(value = "/change-password", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUBSCRIBER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request.username(), request.currentPassword(), request.newPassword());
        log.info("Password changed for user: {}", request.username());
        return ResponseEntity.ok(new ApiResponse<>(true, "Password changed successfully", null));
    }

    @PutMapping(value = "/change-role", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> changeUserRole(@Valid @RequestBody RoleChangeRequest request) {
        User changeUserRole = authService.changeUserRole(request.username(), request.userRole());
        log.info("User role changed for: {}", changeUserRole.getUsername());

        return ResponseEntity.ok(new ApiResponse<>(true, "Role changed successfully", changeUserRole.getRole().toString()));
    }

    @DeleteMapping(value = "/delete-account", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUBSCRIBER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteAccount(@RequestParam String username,
                                                             @AuthenticationPrincipal CustomUserDetails currentUser) {
        // Allow only admins to delete their own account
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            log.warn("Unauthorized delete attempt by '{}' for account '{}'", currentUser.getUsername(), username);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Not authorized to delete this account", null));
        }

        authService.deleteUserAccount(username);
        log.info("Account deleted by '{}': {}", currentUser.getUsername(), username);
        return ResponseEntity.ok(new ApiResponse<>(true, "Account deleted successfully", null));
    }


    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping(value = "/admin-only", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> adminEndpoint(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated", null));
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String ipAddress = request.getRemoteAddr();
        authService.logAdminAccess(userDetails.getUsername(), ipAddress);
        log.info("Admin access granted for: {}", userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(true, "Admin access granted", null));
    }
}

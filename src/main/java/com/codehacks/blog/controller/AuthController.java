package com.codehacks.blog.controller;

import com.codehacks.blog.config.RateLimit;
import com.codehacks.blog.dto.LoginRequest;
import com.codehacks.blog.dto.LoginResponseDTO;
import com.codehacks.blog.dto.PasswordChangeRequest;
import com.codehacks.blog.dto.RegisterRequest;
import com.codehacks.blog.dto.RoleChangeRequest;
import com.codehacks.blog.dto.UserDTO;
import com.codehacks.blog.exception.TokenExpirationException;
import com.codehacks.blog.exception.UserAccountException;
import com.codehacks.blog.model.CustomUserDetails;
import com.codehacks.blog.service.AuthService;
import com.codehacks.blog.service.TokenService;
import com.codehacks.blog.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    @Autowired
    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping(value = "/register", produces = "application/json")
    @Operation(summary = "Register new user")
    @PreAuthorize("permitAll()")
    @RateLimit(maxRequests = 50, timeWindowMinutes = 5)
    public ResponseEntity<UserDTO> register(@RequestBody @Valid RegisterRequest request) {
        UserDTO savedUser = authService.registerUser(request.toUser());
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping(value = "/login", produces = "application/json")
    @PreAuthorize("permitAll()")
    @RateLimit(maxRequests = 50, timeWindowMinutes = 5)
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequest request) throws TokenExpirationException {
        String token;
        if (!tokenService.hasExistingToken(request.email())) {
            token = authService.authenticate(request.email(), request.password());
            tokenService.storeToken(request.email(), token);
        } else {
            String existingToken = tokenService.getExistingToken(request.email());
            boolean validToken = tokenService.isTokenValid(request.password(), existingToken);
            if (validToken) {
                token = existingToken;
            } else {
                token = authService.authenticate(request.email(), request.password());
                tokenService.storeToken(request.email(), token);
            }
        }
        LoginResponseDTO loginResponse = new LoginResponseDTO(token, "Login successful");
        return ResponseEntity.ok(loginResponse);
    }

    @PutMapping(value = "/change-password", produces = "application/json")
    @PreAuthorize("hasAnyRole('SUBSCRIBER', 'ADMIN')")
    @RateLimit(maxRequests = 50, timeWindowMinutes = 5)
    public ResponseEntity<String> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        try {
            authService.changePassword(request.username(), request.currentPassword(), request.newPassword());
            return ResponseEntity.ok("Password changed successfully");
        } catch (UserAccountException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping(value = "/change-role", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> changeUserRole(@Valid @RequestBody RoleChangeRequest request) {
        try {
            authService.changeUserRole(request.username(), request.userRole());
            return ResponseEntity.ok("Role changed successfully");
        } catch (UserAccountException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping(value = "/delete-account", produces = "application/json")
    @PreAuthorize("hasAnyRole('SUBSCRIBER', 'ADMIN')")
    public ResponseEntity<String> deleteAccount(@RequestParam String username, @AuthenticationPrincipal UserDetails userDetails) {
        if (!authService.canUserDeleteAccount(username, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to delete this account");
        }
        authService.deleteUserAccount(username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/logout", produces = "application/json")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> logout(@AuthenticationPrincipal UserDetails userDetails) {
        tokenService.invalidateToken(userDetails.getUsername());
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/admin-only")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(maxRequests = 50, timeWindowMinutes = 5)
    public ResponseEntity<String> adminEndpoint(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String ipAddress = request.getRemoteAddr();
        authService.logAdminAccess(userDetails.getUsername(), ipAddress);
        return ResponseEntity.ok("Admin access granted");
    }
}

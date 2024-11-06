package com.codehacks.blog.controller;

import com.codehacks.blog.exception.TokenExpirationException;
import com.codehacks.blog.model.Role;
import com.codehacks.blog.model.User;
import com.codehacks.blog.service.AuthService;
import com.codehacks.blog.service.TokenService;
import com.codehacks.blog.util.Constants;
import com.codehacks.blog.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.AUTH_PATH)
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(AuthService authService, TokenService tokenService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping(value = "/register", produces = "application/json")
    public ResponseEntity<User> register(@RequestBody User user) {
        User savedUser = authService.registerUser(user);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<String> login(@RequestBody User user) throws TokenExpirationException {
        if (!tokenService.hasExistingToken(user.getUsername())) {
            String token = authService.authenticate(user.getUsername(), user.getPassword());
            tokenService.storeToken(user.getUsername(), token);
            return ResponseEntity.ok("Login Successful");
        }
        String existingToken = tokenService.getExistingToken(user.getUsername());
        boolean validToken = tokenService.isTokenValid(user.getUsername(), existingToken);
        if (!validToken) {
            ResponseEntity.ok("Session has expired.");
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/change-password", produces = "application/json")
    public ResponseEntity<String> changePassword(
            @RequestParam String username,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        authService.changePassword(username, currentPassword, newPassword);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PutMapping(value = "/change-role", produces = "application/json")
    public ResponseEntity<String> changeUserRole(@RequestParam String username,
                                                 @RequestParam Role userRole) {
        User user = authService.changeUserRole(username, userRole);
        return ResponseEntity.ok("Role changed successfully");
    }

    @DeleteMapping(value = "/delete-account", produces = "application/json")
    public ResponseEntity<String> deleteAccount(@RequestParam String username) {
        try {
            authService.deleteUserAccount(username);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @PostMapping(value = "/logout", produces = "application/json")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        String jwtToken = token.substring(7); // Remove "Bearer " prefix
        String username = jwtUtil.extractUsername(jwtToken);
        tokenService.invalidateToken(username);
        return ResponseEntity.ok("Logged out successfully");
    }
}

package com.codehacks.blog.controller;

import com.codehacks.blog.model.User;
import com.codehacks.blog.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/register", produces = "application/json")
    public ResponseEntity<User> register(@RequestBody User user) {
        User savedUser = authService.register(user);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<String> login(@RequestBody User user) {
        String token = authService.authenticate(user.getUsername(), user.getPassword());
        return ResponseEntity.ok(token);
    }

    @PutMapping(value = "/change-password", produces = "application/json")
    public ResponseEntity<String> changePassword(
            @RequestParam String username,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        authService.changePassword(username, currentPassword, newPassword);
        return ResponseEntity.ok("Password changed successfully");
    }

    @DeleteMapping(value = "/delete-account", produces = "application/json")
    public ResponseEntity<String> deleteAccount(@RequestParam String username) {
        try {
            authService.deleteAccount(username);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }
}

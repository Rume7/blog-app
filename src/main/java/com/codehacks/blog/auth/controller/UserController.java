package com.codehacks.blog.auth.controller;

import com.codehacks.blog.auth.dto.ApiResponse;
import com.codehacks.blog.auth.model.User;
import com.codehacks.blog.auth.repository.UserRepository;
import com.codehacks.blog.util.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(Constants.USERS_PATH)
@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600, allowCredentials = "true")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponse<>(true, "No users", null)
            );
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "All users", users));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@RequestParam String email) {
        Optional<User> currentUser = userRepository.findByEmail(email);
        return currentUser.map(user -> ResponseEntity.status(HttpStatus.OK)
                        .body(new ApiResponse<>(true, "Success", user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
} 
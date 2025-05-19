package com.codehacks.blog.auth.controller;

import com.codehacks.blog.auth.dto.ApiResponse;
import com.codehacks.blog.auth.dto.UserDTO;
import com.codehacks.blog.auth.mapper.UserMapper;
import com.codehacks.blog.auth.model.CustomUserDetails;
import com.codehacks.blog.auth.model.User;
import com.codehacks.blog.auth.service.UserDetailsServiceImpl;
import com.codehacks.blog.util.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(Constants.USERS_PATH)
@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600, allowCredentials = "true")
public class UserController {

    private final UserDetailsServiceImpl userDetailsService;

    public UserController(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userDetailsService.findAllUsers();
        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponse<>(true, "No users", null)
            );
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "All users", users));
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<User> currentUser = userDetailsService.findUserByEmail(userDetails.getEmail());
        return currentUser
                .map(user -> {
                    UserDTO userDTO = new UserMapper().apply(user);
                    return ResponseEntity.ok(new ApiResponse<>(true, "Success", userDTO));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
} 
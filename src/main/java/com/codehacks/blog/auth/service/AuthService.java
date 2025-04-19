package com.codehacks.blog.auth.service;

import com.codehacks.blog.auth.dto.UserDTO;
import com.codehacks.blog.auth.exception.UserAccountException;
import com.codehacks.blog.auth.mapper.UserMapper;
import com.codehacks.blog.auth.model.CustomUserDetails;
import com.codehacks.blog.auth.model.Role;
import com.codehacks.blog.auth.model.User;
import com.codehacks.blog.auth.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final AdminService adminService;
    private final PasswordEncoder passwordEncoder;

    public String authenticate(CustomUserDetails customUserDetails) {
        log.info("AuthService: Starting authentication for email: {}", customUserDetails.getEmail());
        Optional<User> optionalUser = userRepository.findByUsername(customUserDetails.getUsername());

        if (optionalUser.isEmpty()) {
            log.error("AuthService: User not found with username: {}", customUserDetails.getUsername());
            throw new UserAccountException("Invalid login credentials");
        }

        User user = optionalUser.get();
        log.info("AuthService: User found - Username: {}, Email: {}", user.getUsername(), user.getEmail());

        return generateToken(customUserDetails);
    }

    private String generateToken(UserDetails userDetails) {
        Optional<User> optionalUser = userRepository.findByUsername(userDetails.getUsername());
        if (optionalUser.isEmpty()) {
            throw new UserAccountException("User not found");
        }

        User user = optionalUser.get();

        if (tokenService.hasExistingToken(user.getEmail())) {
            return tokenService.getToken(user.getEmail());
        }

        return tokenService.generateToken(user);
    }

    public UserDTO registerUser(User user, String rawPassword) {
        log.info("Registration details - Username: {}, Email: {}, Role: {}",
                user.getUsername(), user.getEmail(), user.getRole());

        if (checkIfUsernameExists(user)) {
            log.error("Username already exists: {}", user.getUsername());
            throw new UserAccountException(user.getUsername() + " already exist");
        }
        if (checkIfEmailExists(user.getEmail())) {
            log.error("Email already exists: {}", user.getEmail());
            throw new UserAccountException(user.getEmail() + " already exist");
        }

        User newUser = new User();
        newUser.setUsername(user.getUsername());
        String encodedPassword = passwordEncoder.encode(rawPassword);
        newUser.setPassword(encodedPassword);
        newUser.setEmail(user.getEmail());
        newUser.setRole(user.getRole());
        newUser.setEnabled(true);

        User savedUser = userRepository.save(newUser);
        log.info("User saved successfully - ID: {}, Username: {}, Email: {}, Role: {}",
                savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole());

        tokenService.generateToken(newUser);
        return userMapper.apply(savedUser);
    }

    private boolean checkIfUsernameExists(User user) {
        Optional<User> userFound = userRepository.findByUsername(user.getUsername());
        return userFound.isPresent();
    }

    private boolean checkIfEmailExists(String userEmail) {
        Optional<User> userFound = userRepository.findByEmail(userEmail);
        return userFound.isPresent();
    }

    public void logout(String email) {
        tokenService.invalidateToken(email);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserAccountException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UserAccountException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User changeUserRole(String username, Role role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserAccountException(username + " not found"));

            user.setRole(role);
            return userRepository.save(user);
        }

        throw new UserAccountException("FORBIDDEN: You are not authorized");
    }

    public void deleteUserAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserAccountException("User account not found"));

        if (user.getRole().equals(Role.ADMIN)) {
            userRepository.delete(user);
        }
    }

    public void logAdminAccess(String email, String ipAddress) {
        adminService.logAdminAccess(email, ipAddress);
    }

    public void reportUnauthorizedAdminAccess(String email, String ipAddress) {
        adminService.reportUnauthorizedAdminAccess(email, ipAddress);
    }

    public boolean canUserDeleteAccount(String username, UserDetails userDetails) {
        return userDetails.getUsername().equals(username)
                || userDetails.getAuthorities().contains(new SimpleGrantedAuthority(Role.ADMIN.name()))
                || userDetails.getAuthorities().contains(new SimpleGrantedAuthority(Role.SUBSCRIBER.name()));
    }
}
package com.codehacks.blog.service;

import com.codehacks.blog.dto.UserDTO;
import com.codehacks.blog.exception.UserAccountException;
import com.codehacks.blog.mapper.UserMapper;
import com.codehacks.blog.model.Role;
import com.codehacks.blog.model.User;
import com.codehacks.blog.repository.UserRepository;
import com.codehacks.blog.util.JwtUtil;
import lombok.AllArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final AdminService adminService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserAccountException("Invalid login credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UserAccountException("Invalid password");
        }

        return jwtUtil.generateToken(user.getEmail());
    }

    public UserDTO registerUser(User user) {
        if (checkIfUsernameExists(user)) {
            throw new UserAccountException(user.getUsername() + " already exist");
        }
        if (checkIfEmailExists(user)) {
            throw new UserAccountException(user.getEmail() + " already exist");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return userMapper.apply(savedUser);
    }

    private boolean checkIfUsernameExists(User user) {
        Optional<User> userFound = userRepository.findByUsername(user.getUsername());
        return userFound.isPresent();
    }

    private boolean checkIfEmailExists(User user) {
        Optional<User> userFound = userRepository.findByEmail(user.getEmail());
        return userFound.isPresent();
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserAccountException(username + " not found"));
        user.setRole(role);
        return userRepository.save(user);
    }

    public void deleteUserAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserAccountException("User account not found"));

        userRepository.delete(user);
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
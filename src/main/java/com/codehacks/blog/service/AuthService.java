package com.codehacks.blog.service;

import com.codehacks.blog.exception.UserAccountNotFound;
import com.codehacks.blog.model.Role;
import com.codehacks.blog.model.User;
import com.codehacks.blog.repository.UserRepository;
import com.codehacks.blog.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;

    @Autowired
    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserAccountNotFound("Invalid login credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UserAccountNotFound("Invalid password");
        }

        return jwtUtil.generateToken(username);
    }

    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserAccountNotFound("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UserAccountNotFound("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User changeUserRole(String username, Role role) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserAccountNotFound(username + " not found"));
        user.setRole(role);
        return userRepository.save(user);
    }

    public void deleteUserAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserAccountNotFound("User account not found"));

        userRepository.delete(user);
    }
}

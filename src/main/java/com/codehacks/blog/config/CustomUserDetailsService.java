package com.codehacks.blog.config;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // In-memory user storage for demonstration
    private static final Map<String, String> USER_STORE = new HashMap<>();

    static {
        // Example user: username = "user", password = "password"
        USER_STORE.put("user", "$2a$10$7Qwn/8o/7u1D6MHNK4EZ7eRAuO.c0QaE/j2B0VdLZ91J0ynW2zWfK"); // BCrypt password for "password"
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!USER_STORE.containsKey(username)) {
            throw new UsernameNotFoundException("User not found");
        }

        String password = USER_STORE.get(username);
        return new User(username, password, new ArrayList<>()); // No authorities for simplicity
    }
}

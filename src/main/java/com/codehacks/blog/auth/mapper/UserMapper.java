package com.codehacks.blog.auth.mapper;

import com.codehacks.blog.auth.dto.UserDTO;
import com.codehacks.blog.auth.model.User;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class UserMapper implements Function<User, UserDTO> {

    @Override
    public UserDTO apply(User user) {
        return new UserDTO(user.getEmail(), user.getUsername());
    }
}
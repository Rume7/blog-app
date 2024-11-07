package com.codehacks.blog.mapper;

import com.codehacks.blog.dto.UserDTO;
import com.codehacks.blog.model.User;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class UserMapper implements Function<User, UserDTO> {

    @Override
    public UserDTO apply(User user) {
        return new UserDTO(user.getEmail(), user.getUsername());
    }
}
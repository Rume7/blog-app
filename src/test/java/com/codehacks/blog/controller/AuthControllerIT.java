package com.codehacks.blog.controller;

import com.codehacks.blog.model.User;
import com.codehacks.blog.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private ObjectMapper objectMapper;

    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        postgresContainer.start();
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        objectMapper = new ObjectMapper();
    }

    @BeforeAll
    static void startContainer() {
        postgresContainer.start();
    }

    @AfterAll
    static void stopContainer() {
        postgresContainer.stop();
    }

    @Test
    void testRegisterUser() throws Exception {
        // Given
        User user = new User();
        user.setUsername("testUser");
        user.setPassword("password");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginUser() throws Exception {
        // Given
        User user = new User();
        user.setUsername("testUser");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        userRepository.save(user);

        String jsonRequest = "{\"username\":\"testUser\",\"password\":\"password\"}";

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON) // Correctly set Content-Type
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty()); // Expecting a non-empty response
    }

    @Test
    void testChangePasswordSuccess() throws Exception {
        // Given
        User user = new User();
        user.setUsername("testUser");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        userRepository.save(user);

        String newPassword = "newpassword";

        // When
        mockMvc.perform(put("/api/auth/change-password")
                        .param("username", user.getUsername())
                        .param("currentPassword", "password")
                        .param("newPassword", newPassword)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"password\", \"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isOk());

        // Then
        User updatedUser = userRepository.findByUsername("testUser").orElseThrow();
        assert(new BCryptPasswordEncoder().matches(newPassword, updatedUser.getPassword()));
    }

    @Test
    void testChangePasswordInvalidCurrent() throws Exception {
        // Given
        User user = new User();
        user.setUsername("testUser");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        userRepository.save(user);

        // When
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrongpassword\", \"newPassword\":\"newpassword\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteAccount() throws Exception {
        // Given
        User user = new User();
        user.setUsername("testUser");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        userRepository.save(user);

        // When
        mockMvc.perform(delete("/api/auth/delete-account")
                        .param("username", "testUser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Then
        assert(userRepository.findByUsername("testUser").isEmpty());
    }

    @Test
    void testDeleteAccountNotFound() throws Exception {
        // Given
        User user = new User();
        user.setUsername("testUser");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        userRepository.save(user);
        String nonExistentUsername = "nonexistentuser";

        // When & Then
        mockMvc.perform(delete("/api/auth/delete-account")
                        .param("username", nonExistentUsername))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("User not found")));
    }
}
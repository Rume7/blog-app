package com.codehacks.blog.integrationtests;

import com.codehacks.blog.auth.controller.AuthController;
import com.codehacks.blog.auth.model.User;
import com.codehacks.blog.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
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
        user.setEmail("user@example.com");

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
        user.setEmail("user@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        userRepository.save(user);

        String jsonRequest = "{\"email\":\"user@example.com\",\"password\":\"password\"}";

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
        user.setEmail("user@example.com");
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
        User updatedUser = userRepository.findByEmail("user@example.com").orElseThrow();
        assert (new BCryptPasswordEncoder().matches(newPassword, updatedUser.getPassword()));
    }

    @Test
    void testChangePasswordInvalidCurrent() throws Exception {
        // Given
        User user = new User();
        user.setUsername("testUser");
        user.setEmail("user@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        userRepository.save(user);

        // When
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrongpassword\", \"newPassword\":\"newpassword\", \"email\":\"user@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteAccount() throws Exception {
        // Given
        User user = new User();
        user.setUsername("testUser");
        user.setEmail("user@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        userRepository.save(user);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken("testUser", null, Collections.emptyList());
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        mockMvc.perform(delete("/api/auth/delete-account")
                        .param("username", "testUser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Then
        assert (userRepository.findByEmail("user@example.com").isEmpty());
    }

    @Test
    void testDeleteAccountNotFound() throws Exception {
        // Given
        User user = new User();
        user.setUsername("testUser");
        user.setEmail("user@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        userRepository.save(user);
        String nonExistentUsername = "nonexistentuser";

        // Set up security context
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken(nonExistentUsername, null, Collections.emptyList());
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        mockMvc.perform(delete("/api/auth/delete-account")
                        .param("username", nonExistentUsername))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("User not found")));
    }
}
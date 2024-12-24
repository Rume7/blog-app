package com.codehacks.blog.config;

import com.codehacks.blog.dto.PasswordChangeRequest;
import com.codehacks.blog.dto.RoleChangeRequest;
import com.codehacks.blog.model.Role;
import com.codehacks.blog.model.User;
import com.codehacks.blog.repository.UserRepository;
import com.codehacks.blog.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SecurityConfigTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public BCryptPasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("blog_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clear any pre-existing users
        userRepository.deleteAll();

        // Create and save a test user
        User testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("testuser@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setRole(Role.USER);
        userRepository.save(testUser);

        // Create and save an admin user
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("adminPassword"));
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    void givenUserWithRoleUser_whenAccessRestrictedEndpoint_thenForbidden() throws Exception {
        mockMvc.perform(get(Constants.AUTH_PATH + "/delete-account")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void givenUserWithAdminRole_whenAccessAdminEndpoint_thenOk() throws Exception {
        RoleChangeRequest roleChangeRequest = new RoleChangeRequest("testUser", Role.SUBSCRIBER);

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(roleChangeRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void givenNoAuth_whenAccessRestrictedEndpoint_thenUnauthorized() throws Exception {
        mockMvc.perform(put(Constants.AUTH_PATH + "/change-password")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenValidCorsConfig_whenMakingCorsRequest_thenPass() throws Exception {
        mockMvc.perform(options(Constants.AUTH_PATH + "/login")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void givenInvalidCorsConfig_whenMakingCorsRequest_thenFail() throws Exception {
        mockMvc.perform(options(Constants.AUTH_PATH + "/login")
                        .header("Origin", "https://untrusted-domain.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testSecurityConfigurationWithAdminRole() throws Exception {
        RoleChangeRequest roleChangeRequest = new RoleChangeRequest("testUser", Role.SUBSCRIBER);

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(roleChangeRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    void testSecurityConfigurationWithUserRole() throws Exception {
        PasswordChangeRequest passwordChangeRequest = PasswordChangeRequest.builder()
                .username("testUser")
                .currentPassword("password")
                .newPassword("newPass@123X")
                .build();

        mockMvc.perform(put(Constants.AUTH_PATH + "/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(passwordChangeRequest)))
                .andExpect(status().isOk());
    }
}
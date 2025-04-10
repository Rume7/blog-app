package com.codehacks.blog.integrationtests;

import com.codehacks.blog.post.model.Author;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.auth.model.User;
import com.codehacks.blog.post.repository.BlogRepository;
import com.codehacks.blog.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BlogControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private UserRepository userRepository;

    private Author testAuthor;

    private ObjectMapper objectMapper;

    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        blogRepository.deleteAll();
        userRepository.deleteAll();

        testAuthor = new Author("Test", "Author");

        // Create a user for authentication
        User user = new User();
        user.setUsername("testUser");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        userRepository.save(user);
    }

    @BeforeAll
    static void startContainer() {
        if (!postgresContainer.isRunning()) {
            postgresContainer.start();
        }
    }

    @AfterAll
    static void stopContainer() {
        postgresContainer.stop();
    }

    @Test
    void testGetAllPosts() throws Exception {
        // Given
        String token = getToken();

        Post post1 = new Post("Title 1", "Content 1", testAuthor);
        Post post2 = new Post("Title 2", "Content 2", testAuthor);
        blogRepository.save(post1);
        blogRepository.save(post2);

        // When & Then
        mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testGetPostById() throws Exception {
        // Given
        String token = getToken();

        Post post = new Post("Title", "Content", testAuthor);
        Post savedPost = blogRepository.save(post);

        // When & Then
        mockMvc.perform(get("/api/posts/{id}", savedPost.getId())
                        .header("Authorization", "Bearer " + token))
//                        .param("id", String.valueOf(post.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Title"))
                .andExpect(jsonPath("$.content").value("Content"));
    }

    @NotNull
    private String getToken() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testUser\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void testCreatePost() throws Exception {
        // Given
        String token = getToken();
        Post post = new Post("New Title", "New Content", testAuthor);

        // When & Then
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.content").value("New Content"));
    }

    @Test
    void testUpdatePost() throws Exception {
        // Given
        String token = getToken();

        Post post = new Post("Old Title", "Old Content", testAuthor);
        Post savedPost = blogRepository.save(post);
        Post updatedPost = new Post("Updated Title", "Updated Content", testAuthor);

        // When & Then
        mockMvc.perform(put("/api/posts/{id}", savedPost.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPost)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"));
    }

    @Test
    void testDeletePost() throws Exception {
        // Given
        String token = getToken();
        Post post = new Post("Title", "Content", testAuthor);
        Post savedPost = blogRepository.save(post);

        // When & Then
        mockMvc.perform(delete("/api/posts/{id}", savedPost.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/posts/{id}", savedPost.getId()))
                .andExpect(status().isNotFound());
    }
}
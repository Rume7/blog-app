package com.codehacks.blog.controller;

import com.codehacks.blog.config.SecurityConfig;
import com.codehacks.blog.exception.InvalidPostException;
import com.codehacks.blog.model.Author;
import com.codehacks.blog.model.CustomUserDetails;
import com.codehacks.blog.model.Post;
import com.codehacks.blog.model.Role;
import com.codehacks.blog.service.BlogService;
import com.codehacks.blog.service.TokenService;
import com.codehacks.blog.util.Constants;
import com.codehacks.blog.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableAspectJAutoProxy
@WebMvcTest(BlogController.class)
@Import({SecurityConfig.class, BlogService.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BlogControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private BlogService blogService;

    @MockBean
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testCreatePostWithValue() throws InvalidPostException, Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);

        Post post = new Post("Blog Title", "This is the content of the post");
        Author author = new Author("Larry", "Fink");
        post.setAuthor(author);

        when(blogService.createPost(any(Post.class))).thenReturn(post);

        ResultActions resultActions = mockMvc.perform(post(Constants.BLOG_PATH + "/create")
                        .with(user(userDetails))
                        .param("username", username)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andDo(print());

        resultActions.andExpect(status().isOk());
        verify(blogService, times(1)).createPost(post);
    }

    private UserDetails createUserDetails(String username, Role role) {
        return new CustomUserDetails(username, "Register123Password", "user@example.com",
                role, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name())), true);
    }

    @Test
    void createPostWithIncorrectTitleLength() throws InvalidPostException, Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);

        Post newPost = new Post("Title", "This is a post you would like to read");

        when(blogService.createPost(newPost)).thenThrow(new InvalidPostException("Title length is too short"));

        ResultActions resultActions = mockMvc.perform(post(Constants.BLOG_PATH + "/create")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPost)))
                .andDo(print());

        resultActions.andExpect(jsonPath("$.message").value("Title length is too short"));
        resultActions.andExpect(status().isBadRequest());
        verify(blogService, times(1)).createPost(newPost);
    }
}
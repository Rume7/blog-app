package com.codehacks.blog.controller;

import com.codehacks.blog.config.SecurityConfig;
import com.codehacks.blog.exception.InvalidPostException;
import com.codehacks.blog.model.Author;
import com.codehacks.blog.model.CustomUserDetails;
import com.codehacks.blog.model.Post;
import com.codehacks.blog.model.Role;
import com.codehacks.blog.service.AuthService;
import com.codehacks.blog.service.BlogService;
import com.codehacks.blog.service.TokenService;
import com.codehacks.blog.util.Constants;
import com.codehacks.blog.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableAspectJAutoProxy
@ExtendWith(MockitoExtension.class)
@WebMvcTest(BlogController.class)
@Import({SecurityConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BlogControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private BlogService blogService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserDetailsService userDetailsService;

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Blog Title"))
                .andExpect(jsonPath("$.data.content").value("This is the content of the post"));

        verify(blogService, times(1)).createPost(any(Post.class));
    }

    private UserDetails createUserDetails(String username, Role role) {
        return new CustomUserDetails(username, "Register123Password&", "user@example.com",
                role, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name())), true);
    }

    @Test
    void createPostWithIncorrectTitleLength() throws InvalidPostException, Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);

        Post newPost = new Post("Title", "This is a post you would like to read");

        when(blogService.createPost(any(Post.class)))
                .thenThrow(new InvalidPostException("Title length is too short"));

        ResultActions resultActions = mockMvc.perform(post(Constants.BLOG_PATH + "/create")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPost)))
                .andDo(print());

        resultActions.andExpect(status().isBadRequest());
        verify(blogService, times(1)).createPost(any(Post.class));
    }

    @Test
    void testUpdatePostSuccess() throws Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);
        Long postId = 1L;

        Post updatedPost = new Post("Updated Title", "Updated content");
        Author author = new Author("John", "Doe");
        updatedPost.setAuthor(author);

        when(blogService.updatePost(any(Post.class), eq(postId))).thenReturn(updatedPost);

        ResultActions resultActions = mockMvc.perform(put(Constants.BLOG_PATH + "/update/" + postId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPost)))
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.content").value("Updated content"));

        verify(blogService, times(1)).updatePost(any(Post.class), eq(postId));
    }

    @Test
    void testUpdatePostNotFound() throws Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);
        Long postId = 999L;

        Post updatedPost = new Post("Updated Title", "Updated content");
        when(blogService.updatePost(any(Post.class), eq(postId))).thenReturn(null);

        ResultActions resultActions = mockMvc.perform(put(Constants.BLOG_PATH + "/update/" + postId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPost)))
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(Constants.POST_NOT_FOUND + postId));
    }

    @Test
    void testUpdatePostInvalidId() throws Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);

        Post updatedPost = new Post("Updated Title", "Updated content");

        ResultActions resultActions = mockMvc.perform(put(Constants.BLOG_PATH + "/update/-1")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPost)))
                .andDo(print());

        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    void testUpdatePostInvalidContent() throws Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);
        long postId = 1L;

        Post updatedPost = new Post("", "");

        ResultActions resultActions = mockMvc.perform(put(Constants.BLOG_PATH + "/update/" + postId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPost)))
                .andDo(print());

        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    void testDeletePostSuccess() throws Exception, InvalidPostException {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);
        Long postId = 1L;

        when(blogService.deletePost(postId)).thenReturn(true);

        ResultActions resultActions = mockMvc.perform(delete(Constants.BLOG_PATH + "/delete/" + postId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions.andExpect(status().isNoContent());
        verify(blogService, times(1)).deletePost(postId);
    }

    @Test
    void testDeletePostNotFound() throws Exception, InvalidPostException {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);
        Long postId = 999L;

        when(blogService.deletePost(postId)).thenReturn(false);

        ResultActions resultActions = mockMvc.perform(delete(Constants.BLOG_PATH + "/delete/" + postId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions.andExpect(status().isNotFound());
        verify(blogService, times(1)).deletePost(postId);
    }

    @Test
    void testDeletePostWithInvalidId() throws Exception {
        String username = "testUser";
        long postId = -1L;
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);

        ResultActions resultActions = mockMvc.perform(delete(Constants.BLOG_PATH + "/delete/" + postId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions.andExpect(status().isNotFound());
    }

    @Test
    void testGetAllPostsSuccess() throws Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);

        Set<Post> posts = new HashSet<>();
        Post post1 = new Post("First Post", "Content 1");
        Post post2 = new Post("Second Post", "Content 2");
        posts.add(post1);
        posts.add(post2);

        when(blogService.getAllPosts()).thenReturn(posts);

        ResultActions resultActions = mockMvc.perform(get(Constants.BLOG_PATH + "/all")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("First Post", "Second Post")))
                .andExpect(jsonPath("$[*].content", containsInAnyOrder("Content 1", "Content 2")));

        verify(blogService, times(1)).getAllPosts();
    }

    @Test
    void testGetAllPostsEmpty() throws Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);

        when(blogService.getAllPosts()).thenReturn(new HashSet<>());

        ResultActions resultActions = mockMvc.perform(get(Constants.BLOG_PATH + "/all")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(blogService, times(1)).getAllPosts();
    }

    @Test
    void testGetAllPostsUnauthorized() throws Exception {
        when(blogService.getAllPosts()).thenReturn(new HashSet<>());

        mockMvc.perform(get(Constants.BLOG_PATH + "/all")
                        .with(anonymous())  // Explicitly set as anonymous user
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetPostByIdSuccess() throws Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);
        Long postId = 1L;

        Post post = new Post("Test Post", "Test Content");
        Author author = new Author("John", "Doe");
        post.setAuthor(author);

        when(blogService.getPostById(postId)).thenReturn(post);

        ResultActions resultActions = mockMvc.perform(get(Constants.BLOG_PATH + "/{id}", postId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Post"))
                .andExpect(jsonPath("$.content").value("Test Content"))
                .andExpect(jsonPath("$.author.firstName").value("John"))
                .andExpect(jsonPath("$.author.lastName").value("Doe"));

        verify(blogService, times(1)).getPostById(postId);
    }

    @Test
    void testGetPostByIdNotFound() throws Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);
        Long postId = 999L;

        when(blogService.getPostById(postId)).thenReturn(null);

        ResultActions resultActions = mockMvc.perform(get(Constants.BLOG_PATH + "/{id}", postId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions.andExpect(status().isNotFound());
        verify(blogService, times(1)).getPostById(postId);
    }

    @Test
    void testGetPostByIdInvalidId() throws Exception {
        String username = "testUser";
        UserDetails userDetails = createUserDetails(username, Role.SUBSCRIBER);

        ResultActions resultActions = mockMvc.perform(get(Constants.BLOG_PATH + "/{id}", -1)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());

        resultActions.andExpect(status().isNotFound());
    }
}
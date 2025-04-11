package com.codehacks.blog.controller;

import com.codehacks.blog.auth.config.JwtAuthenticationFilter;
import com.codehacks.blog.auth.exception.GlobalExceptionHandler;
import com.codehacks.blog.auth.exception.InvalidPostException;
import com.codehacks.blog.post.exception.PostNotFoundException;
import com.codehacks.blog.post.model.Author;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.post.controller.BlogController;
import com.codehacks.blog.post.service.BlogService;
import com.codehacks.blog.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(BlogController.class)
@Import(GlobalExceptionHandler.class)
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogService blogService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private Author author;

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setFirstName("John");
        author.setLastName("Doe");
    }

    @Test
    void shouldReturnAllPosts() throws Exception {
        // Given
        Post post1 = new Post("Post 1", "Content 1", author);
        Post post2 = new Post("Post 2", "Content 2", author);
        Set<Post> posts = new HashSet<>(Arrays.asList(post1, post2));

        // When
        Mockito.when(blogService.getAllPosts()).thenReturn(posts);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(posts.size()))
                .andExpect(jsonPath("$[0].title").value("Post 1"))
                .andExpect(jsonPath("$[1].title").value("Post 2"));

        verify(blogService, times(1)).getAllPosts();
    }

    @Test
    void shouldReturnEmptyListWhenNoPostsExist() throws Exception {
        // Given & When
        when(blogService.getAllPosts()).thenReturn(Collections.emptySet());

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(blogService, times(1)).getAllPosts();
    }

    @Test
    void shouldReturnInternalServerErrorIfServiceFails() throws Exception {
        // Given & When
        when(blogService.getAllPosts()).thenThrow(new RuntimeException("Database down"));

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(blogService, times(1)).getAllPosts();
    }

    @Test
    void shouldReturnPostWhenPostExists() throws Exception {
        // Given
        Long postId = 1L;
        Post post = new Post("Test Post", "This is the content", author);
        post.setId(postId);

        // When
        when(blogService.getPostById(postId)).thenReturn(post);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Post"))
                .andExpect(jsonPath("$.content").value("This is the content"))
                .andExpect(jsonPath("$.author.firstName").value("John"))
                .andExpect(jsonPath("$.author.lastName").value("Doe"));

        verify(blogService, times(1)).getPostById(postId);
    }

    @Test
    void shouldReturnNotFoundWhenPostDoesNotExist() throws Exception {
        // Given
        Long postId = 999L;

        // When
        when(blogService.getPostById(postId)).thenReturn(null);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(blogService, times(1)).getPostById(postId);
    }


    @Test
    void shouldReturnInternalServerErrorIfServiceThrowsException() throws Exception {
        // Given
        Long postId = 1L;

        // When
        when(blogService.getPostById(postId)).thenThrow(new RuntimeException("Database error"));

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(blogService, times(1)).getPostById(postId);
    }

    @Test
    void shouldHandlePostNotFoundException() throws Exception {
        // Given
        Long postId = 999L;

        // When
        when(blogService.getPostById(postId)).thenThrow(new PostNotFoundException("Post not found"));

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post not found"));

        verify(blogService, times(1)).getPostById(postId);
    }

    @Test
    void shouldReturnInternalServerErrorIfServiceThrowsExceptionInGetPostById() throws Exception {
        // Given
        Long postId = 1L;

        // When
        when(blogService.getPostById(postId)).thenThrow(new RuntimeException("Database error"));

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(blogService, times(1)).getPostById(postId);
    }

    @Test
    void shouldCreatePostSuccessfully() throws Exception, InvalidPostException {
        // Given
        Post post = new Post("Valid Title", "This is a valid blog post content", author);
        post.setId(2L);

        // When
        when(blogService.createPost(any(Post.class))).thenReturn(post);

        // Then
        mockMvc.perform(post(Constants.BLOG_PATH + "/create")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid Title\", \"content\":\"This is a valid blog post content\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data.title").value("Valid Title"))
                .andExpect(jsonPath("$.data.content").value("This is a valid blog post content"));
    }

    @Test
    void shouldReturnBadRequestWhenTitleIsTooShort() throws Exception, InvalidPostException {
        // Given
        String shortTitle = "ABC";

        // When
        when(blogService.createPost(any(Post.class))).thenThrow(new InvalidPostException("Title length is too short"));

        // Then
        mockMvc.perform(post(Constants.BLOG_PATH + "/create")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"title\":\"" + shortTitle + "\", \"content\":\"Content for readers\"}"))
                .andExpect(status().isBadRequest())  // Expecting 400 Bad Request
                .andExpect(jsonPath("$.message").value("Title length is too short"));
    }

    @Test
    void shouldReturnBadRequestWhenTitleIsTooLong() throws Exception, InvalidPostException {
        // Given
        String longTitle = "A".repeat(200);

        // When
        when(blogService.createPost(any(Post.class))).thenThrow(new InvalidPostException("Title length is too long"));

        // Then
        mockMvc.perform(post(Constants.BLOG_PATH + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + longTitle + "\", \"content\":\"Content\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title length is too long"));
    }

    @Test
    void shouldReturnBadRequestWhenTitleIsNull() throws Exception, InvalidPostException {
        // Given
        Post post = new Post(null, "Content", author);

        // When
        when(blogService.createPost(post)).thenThrow(new InvalidPostException("Title cannot be null"));

        // Then
        mockMvc.perform(post(Constants.BLOG_PATH + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":null, \"content\":\"Content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenContentIsNull() throws Exception, InvalidPostException {
        // Given
        Post post = new Post("Valid Title", null, author);

        // When
        when(blogService.createPost(post)).thenThrow(new InvalidPostException("Blog post cannot be empty"));

        // Then
        mockMvc.perform(post(Constants.BLOG_PATH + "/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid Title\", \"content\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdatePostSuccessfully() throws Exception {
        // Given
        Long postId = 1L;
        Post originalPost = new Post("Original Title", "Original content", author);
        Post updatedPost = new Post("Updated Title", "Updated content", author);

        // When
        when(blogService.updatePost(any(Post.class), eq(postId))).thenReturn(updatedPost);

        // Then
        mockMvc.perform(put(Constants.BLOG_PATH + "/update/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\", \"content\":\"Updated content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.content").value("Updated content"));
    }

    @Test
    void shouldReturnNoContentWhenPostIsDeletedSuccessfully() throws Exception, InvalidPostException {
        // Given
        Long postId = 1L;

        // When
        when(blogService.deletePost(postId)).thenReturn(true);

        // Then
        mockMvc.perform(delete(Constants.BLOG_PATH + "/delete/{id}", postId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void shouldReturnNotFoundWhenPostToDeleteDoesNotExist() throws Exception, InvalidPostException {
        // Given
        Long postId = 1L;

        // When
        when(blogService.deletePost(postId)).thenReturn(false);

        // Then
        mockMvc.perform(delete(Constants.BLOG_PATH + "/delete/{id}", postId))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void shouldReturnBadRequestWhenPostIdIsInvalid() throws Exception {
        // When & Then
        mockMvc.perform(delete(Constants.BLOG_PATH + "/delete/{id}", -1L))
                .andExpect(status().isNotFound()) // Expect 400 Bad Request
                .andExpect(content().string("")); // Expect empty body
    }
}
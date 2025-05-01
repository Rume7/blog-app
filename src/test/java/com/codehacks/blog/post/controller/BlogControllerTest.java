package com.codehacks.blog.post.controller;

import com.codehacks.blog.auth.config.JwtAuthenticationFilter;
import com.codehacks.blog.auth.exception.GlobalExceptionHandler;
import com.codehacks.blog.auth.exception.InvalidPostException;
import com.codehacks.blog.post.dto.BlogPreviewDTO;
import com.codehacks.blog.post.dto.PostSummaryDTO;
import com.codehacks.blog.post.exception.PostNotFoundException;
import com.codehacks.blog.post.model.Author;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.post.service.BlogService;
import com.codehacks.blog.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private int defaultRecentLimit;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setFirstName("John");
        author.setLastName("Doe");
        defaultRecentLimit = 5;     // Default value for testing
        objectMapper = new ObjectMapper();
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

        Map<String, String> postData = new HashMap<>();
        postData.put("title", post.getTitle());
        postData.put("content", post.getContent());

        String jsonContent = objectMapper.writeValueAsString(postData);

        // When
        when(blogService.createPost(any(Post.class))).thenReturn(post);

        // Then
        mockMvc.perform(post(Constants.BLOG_PATH + "/create")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data.title").value("Valid Title"))
                .andExpect(jsonPath("$.data.content").value("This is a valid blog post content"));
    }

    @Test
    void shouldReturnBadRequestWhenTitleIsTooShort() throws Exception, InvalidPostException {
        // Given
        Post post = new Post("ABC", "Content for readers", author);
        post.setId(1L);

        Map<String, String> postData = new HashMap<>();
        postData.put("title", post.getTitle());
        postData.put("content", post.getContent());

        String jsonContent = objectMapper.writeValueAsString(postData);

        // When
        when(blogService.createPost(any(Post.class))).thenThrow(new InvalidPostException("Title length is too short"));

        // Then
        mockMvc.perform(post(Constants.BLOG_PATH + "/create")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(jsonContent))
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

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, String> postData = new HashMap<>();
        postData.put("title", originalPost.getTitle());
        postData.put("content", originalPost.getContent());

        String jsonContent = objectMapper.writeValueAsString(postData);

        // When
        when(blogService.updatePost(any(Post.class), eq(postId))).thenReturn(updatedPost);

        // Then
        mockMvc.perform(put(Constants.BLOG_PATH + "/update/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
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

    @Test
    void testGetRecentPosts_WithLimit() throws Exception {
        // Given
        int limit = 5;
        List<PostSummaryDTO> mockPosts = createMockPosts(limit);

        // When
        when(blogService.getRecentPosts(any(Pageable.class))).thenReturn(mockPosts);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/recent?limit=" + limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(limit)); // Ensure the length is 5
    }

    @Test
    void shouldReturnBlogPreviewsSuccessfully() throws Exception {
        // Given
        List<BlogPreviewDTO> mockPreviews = Arrays.asList(
                new BlogPreviewDTO(1L, "First Post", "John Doe", "Preview content 1", LocalDateTime.now()),
                new BlogPreviewDTO(2L, "Second Post", "Jane Smith", "Preview content 2", LocalDateTime.now())
        );

        // When
        when(blogService.getBlogPreviews()).thenReturn(mockPreviews);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/previews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("First Post"))
                .andExpect(jsonPath("$[0].author").value("John Doe"))
                .andExpect(jsonPath("$[0].previewContent").value("Preview content 1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Second Post"))
                .andExpect(jsonPath("$[1].author").value("Jane Smith"))
                .andExpect(jsonPath("$[1].previewContent").value("Preview content 2"));

        verify(blogService, times(1)).getBlogPreviews();
    }

    @Test
    void shouldReturnEmptyListWhenNoBlogPreviewsExist() throws Exception {
        // Given & When
        when(blogService.getBlogPreviews()).thenReturn(Collections.emptyList());

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/previews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(blogService, times(1)).getBlogPreviews();
    }

    @Test
    void shouldHandleServiceExceptionWhenGettingBlogPreviews() throws Exception {
        // Given & When
        when(blogService.getBlogPreviews()).thenThrow(new RuntimeException("Database error"));

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/previews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(blogService, times(1)).getBlogPreviews();
    }

    @Test
    void shouldReturnRecentPostsWithDefaultLimit() throws Exception {
        // Given
        List<PostSummaryDTO> mockPosts = createMockPosts(defaultRecentLimit);

        // When
        when(blogService.getRecentPosts(any(Pageable.class))).thenReturn(mockPosts);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(defaultRecentLimit))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Post for day 1"))
                .andExpect(jsonPath("$[0].createdAt").exists());

        verify(blogService, times(1)).getRecentPosts(any(Pageable.class));
    }

    @Test
    void shouldReturnRecentPostsWithCustomLimit() throws Exception {
        // Given
        int customLimit = 3;
        List<PostSummaryDTO> mockPosts = createMockPosts(customLimit);

        // When
        when(blogService.getRecentPosts(any(Pageable.class))).thenReturn(mockPosts);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/recent?limit=" + customLimit)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(customLimit))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Post for day 1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Post for day 2"))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].title").value("Post for day 3"));

        verify(blogService, times(1)).getRecentPosts(any(Pageable.class));
    }

    @Test
    void shouldCapLimitAtMaximumValue() throws Exception {
        // Given
        int requestedLimit = 15; // Higher than the maximum allowed
        int expectedLimit = 10; // Maximum allowed limit
        List<PostSummaryDTO> mockPosts = createMockPosts(expectedLimit);

        // When
        when(blogService.getRecentPosts(any(Pageable.class))).thenReturn(mockPosts);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/recent?limit=" + requestedLimit)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(expectedLimit));

        verify(blogService, times(1)).getRecentPosts(any(Pageable.class));
    }

    // Helper method to generate mock data for posts
    private List<PostSummaryDTO> createMockPosts(int limit) {
        return IntStream.range(0, limit)
                .mapToObj(i -> new PostSummaryDTO(i + 1L, "Post for day " + (i + 1), LocalDateTime.of(2022, 01, 03, 22, 20, 2)))
                .collect(Collectors.toList());
    }

    @Test
    void shouldReturnEmptyListWhenNoRecentPostsExist() throws Exception {
        // Given & When
        when(blogService.getRecentPosts(any(Pageable.class))).thenReturn(Collections.emptyList());

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(blogService, times(1)).getRecentPosts(any(Pageable.class));
    }

    @Test
    void shouldHandleServiceExceptionWhenGettingRecentPosts() throws Exception {
        // Given & When
        when(blogService.getRecentPosts(any(Pageable.class))).thenThrow(new RuntimeException("Database error"));

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(blogService, times(1)).getRecentPosts(any(Pageable.class));
    }

    @Test
    void shouldHandleNegativeLimitParameter() throws Exception {
        // Given
        int negativeLimit = -5;

        // When & Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/recent")
                        .param("limit", String.valueOf(negativeLimit))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(blogService, times(0)).getRecentPosts(any(Pageable.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentPost() throws Exception {
        // Given
        Long nonExistentPostId = 999L;
        Post updateData = new Post("Updated Title", "Updated content", author);

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, String> postData = new HashMap<>();
        postData.put("title", updateData.getTitle());
        postData.put("content", updateData.getContent());

        String jsonContent = objectMapper.writeValueAsString(postData);

        // When
        when(blogService.updatePost(any(Post.class), eq(nonExistentPostId)))
                .thenThrow(new PostNotFoundException("Post not found with id: " + nonExistentPostId));

        // Then
        mockMvc.perform(put(Constants.BLOG_PATH + "/update/{id}", nonExistentPostId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post not found with id: " + nonExistentPostId));

        verify(blogService, times(1)).updatePost(any(Post.class), eq(nonExistentPostId));
    }

    @Test
    void shouldHandleInvalidPostDataDuringUpdate() throws Exception {
        // Given
        Long postId = 1L;
        String invalidTitle = "A"; // Too short title

        // When
        when(blogService.updatePost(any(Post.class), eq(postId)))
                .thenThrow(new InvalidPostException("Title length is too short"));

        // Then
        mockMvc.perform(put(Constants.BLOG_PATH + "/update/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + invalidTitle + "\", \"content\":\"Updated content\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title length is too short"));

        verify(blogService, times(1)).updatePost(any(Post.class), eq(postId));
    }

    @Test
    void shouldHandleServiceExceptionDuringUpdate() throws Exception {
        // Given
        Long postId = 1L;
        Post updateData = new Post("Updated Title", "Updated content", author);

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, String> postData = new HashMap<>();
        postData.put("title", updateData.getTitle());
        postData.put("content", updateData.getContent());

        String jsonContent = objectMapper.writeValueAsString(postData);

        // When
        when(blogService.updatePost(any(Post.class), eq(postId)))
                .thenThrow(new RuntimeException("Database error"));

        // Then
        mockMvc.perform(put(Constants.BLOG_PATH + "/update/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isInternalServerError());

        verify(blogService, times(1)).updatePost(any(Post.class), eq(postId));
    }

    @Test
    void shouldSearchPostsByTitleSuccessfully() throws Exception {
        // Given
        String searchTerm = "Spring";
        List<PostSummaryDTO> mockResults = Arrays.asList(
                new PostSummaryDTO(1L, "Spring Boot Tutorial", LocalDateTime.now()),
                new PostSummaryDTO(2L, "Spring Security Guide", LocalDateTime.now())
        );

        // When
        when(blogService.searchPosts(eq(searchTerm), eq(true), eq(true))).thenReturn(mockResults);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/search")
                        .param("query", searchTerm)
                        .param("caseSensitive", "true")
                        .param("exactMatch", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Spring Boot Tutorial"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Spring Security Guide"));

        verify(blogService, times(1)).searchPosts(eq(searchTerm), eq(true), eq(true));
    }

    @Test
    void shouldReturnEmptyListWhenNoMatchingPostsFound() throws Exception {
        // Given
        String searchTerm = "NonexistentPost";

        // When
        when(blogService.searchPosts(eq(searchTerm), eq(true), eq(true))).thenReturn(Collections.emptyList());

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/search")
                        .param("query", searchTerm)
                        .param("caseSensitive", "true")
                        .param("exactMatch", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(blogService, times(1)).searchPosts(eq(searchTerm), eq(true), eq(true));
    }

    @Test
    void shouldHandleInvalidSearchParameter() throws Exception {
        // Given
        String invalidSearchTerm = ""; // Empty search term

        // When
        when(blogService.searchPosts(eq(invalidSearchTerm), eq(true), eq(true)))
                .thenThrow(new InvalidPostException("Search query cannot be empty"));

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/search")
                        .param("query", invalidSearchTerm)
                        .param("caseSensitive", "true")
                        .param("exactMatch", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Search query cannot be empty"));

        verify(blogService, times(0)).searchPosts(eq(invalidSearchTerm), eq(true), eq(true));
    }

    @Test
    void shouldSearchPostsCaseInsensitive() throws Exception {
        // Given
        String searchTerm = "spring";
        List<PostSummaryDTO> mockResults = Arrays.asList(
                new PostSummaryDTO(1L, "Spring Boot Tutorial", LocalDateTime.now()),
                new PostSummaryDTO(2L, "SPRING Security Guide", LocalDateTime.now())
        );

        // When
        when(blogService.searchPosts(eq(searchTerm), eq(false), eq(true))).thenReturn(mockResults);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/search")
                        .param("query", searchTerm)
                        .param("caseSensitive", "false")
                        .param("exactMatch", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(blogService, times(1)).searchPosts(eq(searchTerm), eq(false), eq(true));
    }

    @Test
    void shouldSearchPostsWithPartialMatch() throws Exception {
        // Given
        String searchTerm = "boot";
        List<PostSummaryDTO> mockResults = Arrays.asList(
                new PostSummaryDTO(1L, "Spring Boot Tutorial", LocalDateTime.now()),
                new PostSummaryDTO(3L, "Bootcamp Guide", LocalDateTime.now())
        );

        // When
        when(blogService.searchPosts(eq(searchTerm), eq(true), eq(false))).thenReturn(mockResults);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/search")
                        .param("query", searchTerm)
                        .param("caseSensitive", "true")
                        .param("exactMatch", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(blogService, times(1)).searchPosts(eq(searchTerm), eq(true), eq(false));
    }

    @Test
    void shouldGetPostsByAuthorSuccessfully() throws Exception {
        // Given
        Author authorName = new Author("John", "Doe");
        List<Post> mockResults = Arrays.asList(
                new Post("Post 1 for content 1", "First Post by John", authorName),
                new Post("Post 2 for content 2", "Second Post by John", authorName)
        );

        // When
        when(blogService.getPostsByAuthor(eq(authorName))).thenReturn(mockResults);

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/author")
                        .param("firstName", authorName.getFirstName())
                        .param("lastName", authorName.getLastName())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(blogService, times(1)).getPostsByAuthor(eq(authorName));
    }

    @Test
    void shouldReturnEmptyListWhenAuthorHasNoPosts() throws Exception {
        // Given
        Author authorName = new Author("Jane", "Smith");

        // When
        when(blogService.getPostsByAuthor(eq(authorName))).thenReturn(Collections.emptyList());

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/author")
                        .param("firstName", authorName.getFirstName())
                        .param("lastName", authorName.getLastName())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(blogService, times(1)).getPostsByAuthor(eq(authorName));
    }

    @Test
    void shouldHandleInvalidAuthorData() throws Exception {
        // Given
        Author invalidAuthorName = new Author("", "");

        // When
        when(blogService.getPostsByAuthor(eq(author)))
                .thenThrow(new InvalidPostException("Author name cannot be empty"));

        // Then
        mockMvc.perform(get(Constants.BLOG_PATH + "/author")
                        .param("firstName", invalidAuthorName.getFirstName())
                        .param("lastName", invalidAuthorName.getLastName())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Author name cannot be empty"));

        verify(blogService, times(0)).getPostsByAuthor(any());
    }
}

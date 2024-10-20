package com.codehacks.blog.service;

import com.codehacks.blog.model.Post;
import com.codehacks.blog.repository.BlogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlogServiceImplTest {

    private BlogRepository blogRepository;
    private BlogServiceImpl blogService;

    @BeforeEach
    void setUp() {
        blogRepository = mock(BlogRepository.class);
        blogService = new BlogServiceImpl(blogRepository);
    }

    @Test
    void testGetAllPosts() {
        // Given
        List<Post> expectedPosts = new ArrayList<>();
        expectedPosts.add(new Post(1L, "Title 1", "Content 1", LocalDateTime.now()));
        expectedPosts.add(new Post(2L, "Title 2", "Content 2", LocalDateTime.now()));
        Set<Post> postSet = blogRepository.findAll().stream().collect(Collectors.toSet());
        when(blogRepository.findAll()).thenReturn(expectedPosts);

        // When
        Set<Post> actualPosts = blogService.getAllPosts();

        // Then
        assertEquals(new HashSet<>(expectedPosts), actualPosts);
        verify(blogRepository, times(2)).findAll();
    }

    @Test
    void testGetAPostFound() {
        // Given
        Post expectedPost = new Post(1L, "Title 1", "Content 1", LocalDateTime.now());
        when(blogRepository.findById(1L)).thenReturn(Optional.of(expectedPost));

        // When
        Post actualPost = blogService.getAPost(1L);

        // Then
        assertEquals(expectedPost, actualPost);
    }

    @Test
    void testGetAPostNotFound() {
        // Given
        when(blogRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> blogService.getAPost(1L));
        assertEquals("Post not found", exception.getMessage());
    }

    @Test
    void testDeletePostSuccess() {
        // Given
        Long postId = 1L;
        when(blogRepository.existsById(postId)).thenReturn(true);

        // When
        Boolean result = blogService.deletePost(postId);

        // Then
        assertTrue(result);
        verify(blogRepository, times(1)).deleteById(postId);
    }

    @Test
    void testDeletePostNotFound() {
        // Given
        Long postId = 1L;
        when(blogRepository.existsById(postId)).thenReturn(false);

        // When
        Boolean result = blogService.deletePost(postId);

        // Then
        assertFalse(result);
        verify(blogRepository, times(0)).deleteById(postId);
    }
}
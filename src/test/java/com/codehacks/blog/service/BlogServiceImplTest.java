package com.codehacks.blog.service;

import com.codehacks.blog.exception.PostNotFoundException;
import com.codehacks.blog.model.Post;
import com.codehacks.blog.repository.BlogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

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
        expectedPosts.add(new Post("Title 1", "Content 1"));
        expectedPosts.add(new Post("Title 2", "Content 2"));
        when(blogRepository.findAll()).thenReturn(expectedPosts);

        // When
        Set<Post> actualPosts = blogService.getAllPosts();

        // Then
        assertEquals(new HashSet<>(expectedPosts), actualPosts);
        verify(blogRepository, times(2)).findAll();
    }

    @Test
    void testGetAllPostsReturnsEmptySet() {
        // Given
        when(blogRepository.findAll()).thenReturn(new ArrayList<>());

        // When
        Set<Post> actualPosts = blogService.getAllPosts();

        // Then
        assertNull( actualPosts);
    }

    @Test
    void testGetAllPostsReturnsSinglePost() {
        // Given
        List<Post> expectedPosts = new ArrayList<>();
        expectedPosts.add(new Post("Title 1", "Content 1"));
        when(blogRepository.findAll()).thenReturn(expectedPosts);

        // When
        Set<Post> actualPosts = blogService.getAllPosts();

        // Then
        assertEquals(new HashSet<>(expectedPosts), actualPosts);
    }

    @Test
    void testGetAllPostsReturnsMultiplePosts() {
        // Arrange
        List<Post> expectedPosts = new ArrayList<>();
        expectedPosts.add(new Post("Title 1", "Content 1"));
        expectedPosts.add(new Post("Title 2", "Content 2"));
        when(blogRepository.findAll()).thenReturn(expectedPosts);

        // Act
        Set<Post> actualPosts = blogService.getAllPosts();

        // Assert
        assertEquals(new HashSet<>(expectedPosts), actualPosts);
    }

    @Test
    void testGetAllPostsIgnoresDuplicatePosts() {
        // Given
        List<Post> expectedPosts = new ArrayList<>();
        Post post1 = new Post("Title 1", "Content 1");
        expectedPosts.add(post1);
        expectedPosts.add(post1);
        when(blogRepository.findAll()).thenReturn(expectedPosts);

        // When
        Set<Post> actualPosts = blogService.getAllPosts();

        // Then
        assertEquals(1, actualPosts.size(), "Expected set to contain only one unique post");
        assertTrue(actualPosts.contains(post1), "Expected post should be present in the set");
    }

    @Test
    void testGetAPostFound() {
        // Given
        Post expectedPost = new Post("Title 1", "Content 1");
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
        PostNotFoundException exception = assertThrows(PostNotFoundException.class, () -> blogService.getAPost(1L));
        assertEquals("Post " + 1L + " was not found", exception.getMessage());
    }

    @Test
    void testGetAPostWithNullId() {
        // Given
        Long postId = null;

        // When & Then
        assertThrows(PostNotFoundException.class, () -> blogService.getAPost(postId));
    }

    @Test
    void testGetAPostWithNegativeId() {
        // Given
        Long postId = -1L;

        // When
        when(blogRepository.findById(postId)).thenReturn(Optional.empty());

        // Then
        RuntimeException exception = assertThrows(PostNotFoundException.class, () -> blogService.getAPost(postId));
        assertEquals("Blog id " + postId + " is invalid", exception.getMessage());
    }

    @Test
    void testGetAPostCallsRepositoryOnce() {
        // Given
        Long postId = 1L;
        Post expectedPost = new Post("Title 1", "Content 1");
        when(blogRepository.findById(postId)).thenReturn(Optional.of(expectedPost));

        // When
        blogService.getAPost(postId);

        // Then
        verify(blogRepository, times(1)).findById(postId);
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

    @Test
    void testUpdatePostSuccess() {
        // Given
        Long blogId = 1L;
        Post existingPost = new Post("Old Title", "Old Content");
        existingPost.setId(blogId);

        Post updatedPost = new Post("New Title", "New Content");
        updatedPost.setId(blogId);

        when(blogRepository.existsById(blogId)).thenReturn(true);
        when(blogRepository.findById(blogId)).thenReturn(Optional.of(existingPost));
        when(blogRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Post actualPost = blogService.updatePost(updatedPost, blogId);

        // Then
        assertNotNull(actualPost);
        assertEquals("New Title", actualPost.getTitle());
        assertEquals("New Content", actualPost.getContent());
        verify(blogRepository, times(1)).save(existingPost);
    }

    @Test
    void testUpdatePostNotFound() {
        // Given
        Long blogId = 2L;
        Post updatedPost = new Post("New Title", "New Content");
        updatedPost.setId(blogId);

        when(blogRepository.existsById(blogId)).thenReturn(false);

        // When
        Post actualPost = blogService.updatePost(updatedPost, blogId);

        // Then
        assertNull(actualPost);
        verify(blogRepository, never()).save(any(Post.class));
    }

    @Test
    void testUpdatePostWithNullPost() {
        // Given
        Long blogId = 1L;
        when(blogRepository.existsById(blogId)).thenReturn(true);

        // When
        Post actualPost = blogService.updatePost(null, blogId);

        // Then
        assertNull(actualPost);
        verify(blogRepository, never()).save(any(Post.class));
    }

    @Test
    void testUpdatePostWithNullId() {
        // Given
        Post updatedPost = new Post("New Title", "New Content");
        updatedPost.setId(null);

        // When
        Post actualPost = blogService.updatePost(updatedPost, null);


        // Then
        assertNull(actualPost);
        verify(blogRepository, never()).save(any(Post.class));
    }

    @Test
    void testUpdatePostUpdatesTimestamp() {
        // Given
        Long blogId = 1L;
        Post existingPost = new Post("Old Title", "Old Content");
        existingPost.setId(blogId);
        Post updatedPost = new Post("New Title", "New Content");
        updatedPost.setId(blogId);

        // When
        when(blogRepository.existsById(blogId)).thenReturn(true);
        when(blogRepository.findById(blogId)).thenReturn(Optional.of(existingPost));
        when(blogRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeUpdate = LocalDateTime.now();

        Post actualPost = blogService.updatePost(updatedPost, blogId);

        // Then
        assertTrue(actualPost.getUpdatedAt().isAfter(beforeUpdate));
    }
}
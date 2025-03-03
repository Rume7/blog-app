package com.codehacks.blog.service;

import com.codehacks.blog.exception.InvalidPostException;
import com.codehacks.blog.exception.PostNotFoundException;
import com.codehacks.blog.model.Author;
import com.codehacks.blog.model.Post;
import com.codehacks.blog.repository.AuthorRepository;
import com.codehacks.blog.repository.BlogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceImplTest {
    
    @Mock
    private BlogRepository blogRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BlogServiceImpl blogService;

    private Post testPost;
    private Author testAuthor;

    @BeforeEach
    void setUp() {
        testAuthor = new Author("Test", "Author");
        testPost = new Post("Test Title", "Test Content");
        testPost.setAuthor(testAuthor);
    }

    @Test
    void shouldCreatePostSuccessfully() throws InvalidPostException {
        // Given & When
        when(authorRepository.save(any(Author.class))).thenReturn(testAuthor);
        when(blogRepository.save(any(Post.class))).thenReturn(testPost);
        Post result = blogService.createPost(testPost);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(testPost.getTitle(), result.getTitle()),
                () -> assertEquals(testPost.getContent(), result.getContent()),
                () -> assertEquals(testPost.getAuthor(), result.getAuthor())
        );
        verify(authorRepository, times(1)).save(any(Author.class));
        verify(blogRepository, times(1)).save(testPost);
    }

    @Test
    void shouldReturnEmptySetWhenNoPostsExist() {
        // Given
        when(blogRepository.findAll()).thenReturn(Collections.emptyList());
        
        // When
        Set<Post> result = blogService.getAllPosts();
        
        // Then
        assertAll(
            () -> assertNotNull(result),
            () -> assertTrue(result.isEmpty())
        );
        verify(blogRepository).findAll();
    }

    @Test
    void shouldReturnUniquePostsWhenDuplicatesExist() {
        // Given
        List<Post> duplicatePosts = Arrays.asList(testPost, testPost);

        // When
        when(blogRepository.findAll()).thenReturn(duplicatePosts);

        Set<Post> result = blogService.getAllPosts();
        
        // Then
        assertAll(
            () -> assertEquals(1, result.size()),
            () -> assertTrue(result.contains(testPost))
        );
        verify(blogRepository, times(1)).findAll();
    }

    @Test
    void shouldUpdateExistingPost() {
        // Given
        Long postId = 1L;
        Post updatedPost = new Post("Updated Title", "Updated Content");

        // When
        when(blogRepository.existsById(postId)).thenReturn(true);
        when(blogRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(blogRepository.save(any(Post.class))).thenReturn(updatedPost);

        Post result = blogService.updatePost(updatedPost, postId);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(updatedPost.getTitle(), result.getTitle()),
                () -> assertEquals(updatedPost.getContent(), result.getContent())
        );
        verify(blogRepository).findById(postId);
        verify(blogRepository).save(any(Post.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentPost() {
        // Given
        Long nonExistentId = 999L;
        Post updatePost = new Post("Title", "Content");

        // When
        PostNotFoundException exception = assertThrows(PostNotFoundException.class,
                () -> blogService.updatePost(updatePost, nonExistentId));

        // Then
        assertEquals("Post not found with id: " + nonExistentId, exception.getMessage());
        verify(blogRepository, never()).save(any(Post.class));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidPosts")
    void shouldHandleInvalidPostsForUpdate(Post invalidPost, Long id) {
        // When & Then
        assertThrows(PostNotFoundException.class,
            () -> blogService.updatePost(invalidPost, id));
    }

    private static Stream<Arguments> provideInvalidPosts() {
        return Stream.of(
            Arguments.of(null, 1L),
            Arguments.of(new Post("", ""), 1L),
            Arguments.of(new Post(null, null), 1L)
        );
    }

    @Test
    void shouldDeletePostSuccessfully() throws InvalidPostException {
        // Given
        Long postId = 1L;
        when(blogRepository.existsById(postId)).thenReturn(true);
        
        // When
        blogService.deletePost(postId);
        
        // Then
        verify(blogRepository, times(1)).deleteById(postId);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentPost() {
        // Given
        Long nonExistentId = 999L;

        // When
        PostNotFoundException exception = assertThrows(PostNotFoundException.class,
                () -> blogService.deletePost(nonExistentId));

        // Then
        assertEquals("Post not found with id: " + nonExistentId, exception.getMessage());
        verify(blogRepository, never()).deleteById(any());
    }

    @Test
    void shouldFindPostById() {
        // Given
        Long postId = 1L;
        when(blogRepository.findById(postId)).thenReturn(Optional.of(testPost));

        // When
        Post result = blogService.getPostById(postId);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(testPost.getTitle(), result.getTitle()),
                () -> assertEquals(testPost.getContent(), result.getContent())
        );
        verify(blogRepository, times(1)).findById(postId);
    }

    @Test
    void shouldFilterPostsByTitle() {
        // Given
        String searchTitle = "Test";
        List<Post> filteredPosts = Arrays.asList(
                new Post("Test Post 1", "Content 1"),
                new Post("Test Post 2", "Content 2")
        );
        when(blogRepository.findByTitleContainingIgnoreCase(searchTitle))
                .thenReturn(filteredPosts);

        // When
        List<Post> results = blogService.searchPostsByTitle(searchTitle);

        // Then
        assertAll(
                () -> assertEquals(2, results.size()),
                () -> assertTrue(results.stream()
                        .allMatch(post -> post.getTitle().contains(searchTitle)))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "ab"})
    void shouldValidatePostTitleLength(String invalidTitle) {
        // Given
        Post invalidPost = new Post(invalidTitle, "Valid Content");

        // When
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.createPost(invalidPost));

        // Then
        assertEquals("Title length is too short", exception.getMessage());
        verify(blogRepository, never()).save(any());
    }

    @Test
    void shouldGetPostsByAuthor() {
        // Given
        Author authorName = new Author("Test", "Author");
        List<Post> authorPosts = Arrays.asList(
                new Post("Post 1", "Content 1"),
                new Post("Post 2", "Content 2")
        );
        when(blogRepository.findByAuthor(authorName)).thenReturn(authorPosts);

        // When
        List<Post> results = blogService.getPostsByAuthor(authorName);

        // Then
        assertEquals(2, results.size());
        verify(blogRepository, times(1)).findByAuthor(authorName);
    }
}

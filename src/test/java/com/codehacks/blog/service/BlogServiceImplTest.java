package com.codehacks.blog.service;

import com.codehacks.blog.auth.exception.InvalidPostException;
import com.codehacks.blog.post.exception.PostNotFoundException;
import com.codehacks.blog.post.model.Author;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.post.repository.AuthorRepository;
import com.codehacks.blog.post.repository.BlogRepository;
import com.codehacks.blog.post.service.BlogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
        testPost = new Post("Test Title", "Test Content", testAuthor);
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
        Author author = new Author("Larry", "Sally");
        Post updatedPost = new Post("Updated Title", "Updated Content", author);

        // When
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
        Post updatePost = new Post("Title", "Content", testAuthor);

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
        Author author = new Author();
        return Stream.of(
                Arguments.of(null, 1L),
                Arguments.of(new Post("", "", author), 1L),
                Arguments.of(new Post(null, null, author), 1L)
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
                new Post("Test Post 1", "Content 1", testAuthor),
                new Post("Test Post 2", "Content 2", testAuthor)
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
    @MethodSource("provideInvalidPostTitles")
    void shouldValidatePostTitleLength(String invalidTitle, String message) {
        // Given
        Post invalidPost = new Post(invalidTitle, "Valid Content", testAuthor);

        // When
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.createPost(invalidPost));

        // Then
        assertEquals(message, exception.getMessage());
        verify(blogRepository, never()).save(any());
    }

    private static Stream<Arguments> provideInvalidPostTitles() {
        return Stream.of(
                Arguments.of("", "Title cannot be null or empty or only whitespace"),
                Arguments.of(" ", "Title cannot be null or empty or only whitespace"),
                Arguments.of("ab", "Title length is too short")
        );
    }

    @Test
    void shouldGetPostsByAuthor() {
        // Given
        Author authorName = new Author("Test", "Author");
        List<Post> authorPosts = Arrays.asList(
                new Post("Post 1", "Content 1", authorName),
                new Post("Post 2", "Content 2", authorName)
        );
        when(blogRepository.findByAuthor(authorName)).thenReturn(authorPosts);

        // When
        List<Post> results = blogService.getPostsByAuthor(authorName);

        // Then
        assertEquals(2, results.size());
        verify(blogRepository, times(1)).findByAuthor(authorName);
    }
}

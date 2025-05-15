package com.codehacks.blog.post.service;

import com.codehacks.blog.auth.exception.InvalidPostException;
import com.codehacks.blog.auth.exception.InvalidSearchQueryException;
import com.codehacks.blog.post.dto.PostSummaryDTO;
import com.codehacks.blog.post.exception.MissingAuthorException;
import com.codehacks.blog.post.exception.PostNotFoundException;
import com.codehacks.blog.post.mapper.PostMapper;
import com.codehacks.blog.post.model.Author;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.post.repository.AuthorRepository;
import com.codehacks.blog.post.repository.PostRepository;
import com.codehacks.blog.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private PostServiceImpl blogService;

    @Mock
    private PostMapper postMapper;

    private Post testPost;
    private Author testAuthor;
    private Pageable pageable;
    private List<Post> posts;
    private List<PostSummaryDTO> postSummaryDTOs;

    @BeforeEach
    void setUp() {
        testAuthor = new Author("Test", "Author", "author@testExample.com");
        testPost = new Post("Test Title", "Test Content", testAuthor);

        pageable = PageRequest.of(0, 7); // Let's assume we want 7 posts for the test case

        // Create a list of sample posts
        posts = List.of(
                new Post("Post for Day 1", "Content of post 1", testAuthor, LocalDateTime.now()),
                new Post("Post for Day 2", "Content of post 2", testAuthor, LocalDateTime.now().minusDays(1)),
                new Post("Post for Day 3", "Content of post 3", testAuthor, LocalDateTime.now().minusDays(2))
        );

        // Map posts to PostSummaryDTO
        postSummaryDTOs = posts.stream()
                .map(post -> new PostSummaryDTO(post.getId(), post.getTitle(), post.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Test
    void shouldCreatePostSuccessfully() throws InvalidPostException {
        // Given & When
        when(authorRepository.save(any(Author.class))).thenReturn(testAuthor);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        Post result = blogService.createPost(testPost);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(testPost.getTitle(), result.getTitle()),
                () -> assertEquals(testPost.getContent(), result.getContent()),
                () -> assertEquals(testPost.getAuthor(), result.getAuthor())
        );
        verify(authorRepository, times(1)).save(any(Author.class));
        verify(postRepository, times(1)).save(testPost);
    }

    @Test
    void shouldReturnEmptySetWhenNoPostsExist() {
        // Given
        when(postRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        Set<Post> result = blogService.getAllPosts();

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty())
        );
        verify(postRepository).findAll();
    }

    @Test
    void shouldReturnUniquePostsWhenDuplicatesExist() {
        // Given
        List<Post> duplicatePosts = Arrays.asList(testPost, testPost);

        // When
        when(postRepository.findAll()).thenReturn(duplicatePosts);

        Set<Post> result = blogService.getAllPosts();

        // Then
        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertTrue(result.contains(testPost))
        );
        verify(postRepository, times(1)).findAll();
    }

    @Test
    void shouldUpdatePostSuccessfully() {
        // Given
        Long postId = 1L;
        Post existingPost = new Post("Original Title", "Original Content", testAuthor);
        Post updatedPost = new Post("Updated Title", "Updated Content", testAuthor);
        existingPost.setId(1L);

        when(postRepository.findByIdWithComments(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post savedPost = invocation.getArgument(0);
            savedPost.setId(postId);
            return savedPost;
        });

        // When
        Post result = blogService.updatePost(updatedPost, postId);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(updatedPost.getTitle(), result.getTitle()),
                () -> assertEquals(updatedPost.getContent(), result.getContent()),
                () -> assertNotNull(result.getUpdatedAt())
        );
        verify(postRepository).findByIdWithComments(postId);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentPost() {
        // Given
        Long nonExistentId = 999L;
        Post updatePost = new Post("Title", "Content", testAuthor);
        when(postRepository.findByIdWithComments(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        PostNotFoundException exception = assertThrows(PostNotFoundException.class,
                () -> blogService.updatePost(updatePost, nonExistentId));

        assertEquals("Post not found with id: " + nonExistentId, exception.getMessage());
        verify(postRepository).findByIdWithComments(nonExistentId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithInvalidId() {
        // Given
        Long invalidId = -1L;
        Post updatePost = new Post("Title", "Content", testAuthor);
        when(postRepository.findByIdWithComments(invalidId)).thenReturn(Optional.empty());

        // When & Then
        PostNotFoundException exception = assertThrows(PostNotFoundException.class,
                () -> blogService.updatePost(updatePost, invalidId));

        assertEquals("Post not found with id: " + invalidId, exception.getMessage());
        verify(postRepository).findByIdWithComments(invalidId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithNullPost() {
        // Given
        Long postId = 1L;

        // When & Then
        assertThrows(PostNotFoundException.class,
                () -> blogService.updatePost(null, postId));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldPreserveAuthorWhenUpdatingPost() {
        // Given
        Long postId = 1L;
        Post existingPost = new Post("Original Title", "Original Content", testAuthor);
        Post updatedPost = new Post("Updated Title", "Updated Content", new Author("New", "Author", "newAuthor@test.com"));
        when(postRepository.findByIdWithComments(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Post result = blogService.updatePost(updatedPost, postId);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(updatedPost.getTitle(), result.getTitle()),
                () -> assertEquals(updatedPost.getContent(), result.getContent()),
                () -> assertEquals(testAuthor.getFirstName(), result.getAuthor().getFirstName()),
                () -> assertEquals(testAuthor.getLastName(), result.getAuthor().getLastName())
        );

        verify(postRepository).findByIdWithComments(postId);
        verify(postRepository).save(any(Post.class));
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
        when(postRepository.existsById(postId)).thenReturn(true);

        // When
        blogService.deletePost(postId);

        // Then
        verify(postRepository, times(1)).deleteById(postId);
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
        verify(postRepository, never()).deleteById(any());
    }

    @Test
    void shouldFindPostById() {
        // Given
        Long postId = 1L;
        when(postRepository.findByIdWithComments(postId)).thenReturn(Optional.of(testPost));

        // When
        Post result = blogService.getPostById(postId);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(testPost.getTitle(), result.getTitle()),
                () -> assertEquals(testPost.getContent(), result.getContent())
        );
        verify(postRepository, times(1)).findByIdWithComments(postId);
    }

    @Test
    void shouldFilterPostsByTitle() {
        // Given
        String searchTitle = "Test";
        List<Post> filteredPosts = Arrays.asList(
                new Post("Test Post 1", "Content 1", testAuthor),
                new Post("Test Post 2", "Content 2", testAuthor)
        );

        when(postRepository.findByTitleContainingIgnoreCase(searchTitle))
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
        verify(postRepository, never()).save(any());
    }

    private static Stream<Arguments> provideInvalidPostTitles() {
        return Stream.of(
                Arguments.of("", "Title is too short"),
                Arguments.of(" ", "Title is too short"),
                Arguments.of("ab", "Title is too short")
        );
    }

    @Test
    void shouldValidatePostContentLength() {
        // Given
        Post post = new Post("Valid Title", "Short", testAuthor);

        // When & Then
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.createPost(post));

        assertEquals("Content is too short", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingPostWithNullContent() {
        // Given
        Post post = new Post("Valid Title", null, testAuthor);

        // When & Then
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.createPost(post));

        assertEquals("Content cannot be null nor empty", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingPostWithWhitespaceOnlyTitle() {
        // Given
        Long postId = 1L;
        Post existingPost = new Post("Original Title", "Original Content", testAuthor);
        Post updatedPost = new Post("   ", "Valid Content", testAuthor);
        existingPost.setId(1L);

        when(postRepository.findByIdWithComments(postId)).thenReturn(Optional.of(existingPost));

        // When & Then
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.updatePost(updatedPost, postId));

        assertEquals("Title is too short", exception.getMessage());

        verify(postRepository).findByIdWithComments(postId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingPostWithWhitespaceOnlyContent() {
        // Given
        Long postId = 1L;
        Post existingPost = new Post("Original Title", "Original Content", testAuthor);
        Post updatedPost = new Post("Valid Title", "   ", testAuthor);
        existingPost.setId(1L);

        when(postRepository.findByIdWithComments(postId)).thenReturn(Optional.of(existingPost));

        // When & Then
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.updatePost(updatedPost, postId));

        assertEquals("Content is too short", exception.getMessage());

        verify(postRepository).findByIdWithComments(postId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldValidateUpdatedTitleLength() {
        // Given
        Long postId = 1L;
        Post existingPost = new Post("Original Title", "Original Content", testAuthor);
        Post updatedPost = new Post("Short", "Updated Content", testAuthor);
        existingPost.setId(1L);

        when(postRepository.findByIdWithComments(postId)).thenReturn(Optional.of(existingPost));

        // When & Then
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.updatePost(updatedPost, postId));

        assertEquals("Title is too short", exception.getMessage());
        verify(postRepository).findByIdWithComments(postId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldValidateUpdatedContentLength() {
        // Given
        Long postId = 1L;
        Post existingPost = new Post("Original Title", "Original Content", testAuthor);
        Post updatedPost = new Post("Valid Title", "Short", testAuthor);
        existingPost.setId(1L);

        when(postRepository.findByIdWithComments(postId)).thenReturn(Optional.of(existingPost));

        // When & Then
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.updatePost(updatedPost, postId));

        assertEquals("Content is too short", exception.getMessage());
        verify(postRepository).findByIdWithComments(postId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldValidateUpdatedContentMaximumLength() {
        // Given
        Long postId = 1L;
        Post existingPost = new Post("Original Title", "Original Content", testAuthor);
        String longContent = "a".repeat(Constants.MAX_CONTENT_LENGTH + 1);
        Post updatedPost = new Post("Valid Title", longContent, testAuthor);
        existingPost.setId(1L);

        when(postRepository.findByIdWithComments(postId)).thenReturn(Optional.of(existingPost));

        // When & Then
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.updatePost(updatedPost, postId));

        assertEquals("Content is too long", exception.getMessage());
        verify(postRepository).findByIdWithComments(postId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldValidateUpdatedTitleMaximumLength() {
        // Given
        Long postId = 1L;
        Post existingPost = new Post("Original Title", "Original Content", testAuthor);
        String longTitle = "a".repeat(Constants.MAX_TITLE_LENGTH + 1);
        Post updatedPost = new Post(longTitle, "Valid Content", testAuthor);
        existingPost.setId(1L);

        when(postRepository.findByIdWithComments(postId)).thenReturn(Optional.of(existingPost));

        // When & Then
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.updatePost(updatedPost, postId));

        assertEquals("Title is too long", exception.getMessage());
        verify(postRepository).findByIdWithComments(postId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldValidateUpdatedTitleIsNotNullOrEmpty() {
        // Given
        Long postId = 1L;
        Post existingPost = new Post("Original Title", "Original Content", testAuthor);
        Post updatedPost = new Post("", "Valid Content", testAuthor);
        existingPost.setId(1L);

        when(postRepository.findByIdWithComments(postId)).thenReturn(Optional.of(existingPost));

        // When & Then
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.updatePost(updatedPost, postId));

        assertEquals("Title is too short", exception.getMessage());
        verify(postRepository).findByIdWithComments(postId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingPostWithNullAuthor() {
        // Given
        Post post = new Post("Valid Title", "Valid content that is long enough to pass validation", null);

        // When & Then
        InvalidPostException exception = assertThrows(InvalidPostException.class,
                () -> blogService.createPost(post));

        assertEquals("Valid author details are required", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    // This method ensure that the author's email is not updated when a post is created or updated.
    // Admin can change author's email in the future.
    @Test
    void shouldHandleExistingAuthorWithDifferentEmail() throws InvalidPostException {
        // Given
        Author existingAuthor = new Author("Test", "Author", "test@example.com");
        Author newAuthor = new Author("Test", "Author", "different@example.com");

        Post post = new Post("Valid Title", "Valid content that is long enough to pass validation", newAuthor);

        when(authorRepository.findByEmail(any())).thenReturn(existingAuthor);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // When
        Post result = blogService.createPost(post);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(post.getTitle(), result.getTitle()),
                () -> assertEquals(post.getContent(), result.getContent()),
                () -> assertEquals(existingAuthor.getEmail(), result.getAuthor().getEmail())
        );

        verify(authorRepository, times(1)).findByEmail(any());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void shouldGetPostsByAuthor() {
        // Given
        Author authorName = new Author("Test", "Author", "test@author.com");
        List<Post> authorPosts = Arrays.asList(
                new Post("Post 1", "Content 1", authorName),
                new Post("Post 2", "Content 2", authorName)
        );
        when(postRepository.findByAuthor(authorName)).thenReturn(authorPosts);

        // When
        List<Post> results = blogService.getPostsByAuthor(authorName);

        // Then
        assertEquals(2, results.size());
        verify(postRepository, times(1)).findByAuthor(authorName);
    }

    @Test
    void testGetRecentPosts_Success() {
        // Given
        when(postRepository.findTopNRecentPostsOrderByCreatedAt(pageable)).thenReturn(posts);


        when(postMapper.toSummary(any(Post.class))).thenReturn(new PostSummaryDTO(1L, "Post for Day 1", LocalDateTime.now()));

        // When
        List<PostSummaryDTO> result = blogService.getRecentPosts(pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Post for Day 1", result.get(0).getTitle());
    }

    @Test
    void testGetRecentPosts_NoPosts() {
        // Given
        when(postRepository.findTopNRecentPostsOrderByCreatedAt(pageable))
                .thenReturn(Collections.emptyList());

        // When
        List<PostSummaryDTO> result = blogService.getRecentPosts(pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRecentPosts_PagableWithDifferentSize() {
        // Given
        List<Post> largeListOfPosts = new ArrayList<>();

        for (int i = 0; i <= 20; i++) {
            final Post post = new Post("Post for Day " + (i + 1), "Content of post " + (i + 1), testAuthor, LocalDateTime.now().minusDays(i));
            largeListOfPosts.add(post);
        }

        Pageable pageable = PageRequest.of(0, 5);

        when(postRepository.findTopNRecentPostsOrderByCreatedAt(any(Pageable.class)))
                .thenReturn(largeListOfPosts.subList(0, 5));

        // When
        List<PostSummaryDTO> result = blogService.getRecentPosts(pageable);

        // Then
        assertNotNull(result);
        assertEquals(5, result.size());
        verify(postRepository, times(1)).findTopNRecentPostsOrderByCreatedAt(any(Pageable.class));
    }

    @Test
    void shouldSearchPostsWithExactMatch() {
        // Given
        String searchQuery = "Spring Boot";
        List<Post> mockPosts = Arrays.asList(
                new Post("Spring Boot", "Content 1", testAuthor),
                new Post("Spring Boot", "Content 2", testAuthor),
                new Post("Spring Framework", "Content 3", testAuthor)
        );
        when(postRepository.findAll()).thenReturn(mockPosts);
        when(postMapper.toSummary(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            return new PostSummaryDTO(post.getId(), post.getTitle(), post.getCreatedAt());
        });

        // When
        Set<PostSummaryDTO> results = blogService.searchPosts(searchQuery, true, true);

        // Then
        assertAll(
                () -> assertNotNull(results),
                () -> assertEquals(2, results.size()),
                () -> assertTrue(results.stream()
                        .allMatch(post -> post.getTitle().equals(searchQuery)))
        );
        verify(postRepository, times(1)).findAll();
        verify(postMapper, times(2)).toSummary(any(Post.class));
    }

    @Test
    void shouldSearchPostsWithPartialMatch() {
        // Given
        String searchQuery = "Spring";
        List<Post> mockPosts = Arrays.asList(
                new Post("Spring Boot", "Content 1", testAuthor),
                new Post("Spring Framework", "Content 2", testAuthor),
                new Post("Java Tutorial", "Content 3", testAuthor)
        );
        when(postRepository.findAll()).thenReturn(mockPosts);
        when(postMapper.toSummary(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            return new PostSummaryDTO(post.getId(), post.getTitle(), post.getCreatedAt());
        });

        // When
        Set<PostSummaryDTO> results = blogService.searchPosts(searchQuery, true, false);

        // Then
        assertAll(
                () -> assertNotNull(results),
                () -> assertEquals(2, results.size()),
                () -> assertTrue(results.stream()
                        .allMatch(post -> post.getTitle().contains(searchQuery)))
        );
        verify(postRepository, times(1)).findAll();
        verify(postMapper, times(2)).toSummary(any(Post.class));
    }

    @Test
    void shouldSearchPostsCaseInsensitive() {
        // Given
        String searchQuery = "spring";
        List<Post> mockPosts = Arrays.asList(
                new Post("Spring Boot", "Content 1", testAuthor),
                new Post("SPRING Framework", "Content 2", testAuthor),
                new Post("Java Tutorial", "Content 3", testAuthor)
        );
        when(postRepository.findAll()).thenReturn(mockPosts);
        when(postMapper.toSummary(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            return new PostSummaryDTO(post.getId(), post.getTitle(), post.getCreatedAt());
        });

        // When
        Set<PostSummaryDTO> results = blogService.searchPosts(searchQuery, false, false);

        // Then
        assertAll(
                () -> assertNotNull(results),
                () -> assertEquals(2, results.size()),
                () -> assertTrue(results.stream()
                        .allMatch(post -> post.getTitle().toLowerCase().contains(searchQuery.toLowerCase())))
        );
        verify(postRepository, times(1)).findAll();
        verify(postMapper, times(2)).toSummary(any(Post.class));
    }

    @Test
    void shouldThrowExceptionWhenSearchQueryIsEmpty() {
        // Given
        String emptyQuery = "";

        // When & Then
        InvalidSearchQueryException exception = assertThrows(InvalidSearchQueryException.class,
                () -> blogService.searchPosts(emptyQuery, true, true));

        assertEquals("Search query cannot be empty", exception.getMessage());
        verify(postRepository, never()).findAll();
    }

    @Test
    void shouldThrowExceptionWhenSearchingWithWhitespaceOnlyQuery() {
        // When & Then
        InvalidSearchQueryException exception = assertThrows(InvalidSearchQueryException.class,
                () -> blogService.searchPosts("   ", true, true));

        assertEquals("Search query cannot be empty", exception.getMessage());
        verify(postRepository, never()).findAll();
    }

    @Test
    void shouldThrowExceptionWhenGettingPostWithNullId() {
        // When & Then
        assertThrows(PostNotFoundException.class,
                () -> blogService.getPostById(null));
        verify(postRepository, never()).findByIdWithComments(any());
    }

    @Test
    void shouldThrowExceptionWhenGettingRecentPostsWithNullPageable() {
        // When & Then
        assertThrows(PostNotFoundException.class,
                () -> blogService.getRecentPosts(null));

        verify(postRepository, never()).findTopNRecentPostsOrderByCreatedAt(any());
    }

    @Test
    void shouldThrowExceptionWhenGettingPostsByNullAuthor() {
        // When & Then
        assertThrows(MissingAuthorException.class,
                () -> blogService.getPostsByAuthor(null));

        verify(postRepository, never()).findByAuthor(any());
    }

    @Test
    void shouldThrowExceptionWhenSearchingWithNullQuery() {
        // When & Then
        InvalidSearchQueryException exception = assertThrows(InvalidSearchQueryException.class,
                () -> blogService.searchPosts(null, true, true));

        assertEquals("Search query cannot be empty", exception.getMessage());
        verify(postRepository, never()).findAll();
    }
}

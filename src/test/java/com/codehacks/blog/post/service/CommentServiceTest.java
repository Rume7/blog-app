package com.codehacks.blog.post.service;

import com.codehacks.blog.auth.exception.UserAccountException;
import com.codehacks.blog.auth.model.User;
import com.codehacks.blog.auth.repository.UserRepository;
import com.codehacks.blog.post.exception.CommentNotFoundException;
import com.codehacks.blog.post.exception.InvalidCommentException;
import com.codehacks.blog.post.exception.InvalidPostIdException;
import com.codehacks.blog.post.exception.PostNotFoundException;
import com.codehacks.blog.post.model.Comment;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.post.repository.PostRepository;
import com.codehacks.blog.post.repository.CommentRepository;
import com.codehacks.blog.post.dto.CommentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCommentById_ExistingComment() {
        // Given
        Long commentId = 1L;
        Long postId = 1L;

        Post post = new Post();
        post.setId(postId);

        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setContent("This is a great post!");
        comment.setAuthor("John Doe");
        comment.setPost(post);
        comment.setCreatedAt(LocalDateTime.now());

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // When
        CommentDto result = commentService.getCommentById(commentId);

        // Then
        assertAll("Check CommentDto properties",
                () -> assertNotNull(result),
                () -> assertEquals(commentId, result.getId(), "Id should be equal"),
                () -> assertEquals("This is a great post!", result.getContent(), "Content should match"),
                () -> assertEquals(1L, result.getPostId(), "Post ID should match")
        );
    }

    @Test
    void testGetCommentById_NonExistingComment() {
        // Given
        Long commentId = 1L;
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // When and Then
        assertThrows(CommentNotFoundException.class, () -> {
            commentService.getCommentById(commentId);
        });
    }

    @Test
    void testDeleteComment_ExistingComment() {
        // Given
        Long commentId = 1L;
        when(commentRepository.existsById(commentId)).thenReturn(true);

        // When
        commentService.deleteComment(commentId);

        // Then
        verify(commentRepository, times(1)).deleteById(commentId);
    }

    @Test
    void testDeleteComment_NonExistingComment() {
        // Given
        Long commentId = 1L;
        when(commentRepository.existsById(commentId)).thenReturn(false);

        // When and Then
        assertThrows(CommentNotFoundException.class, () -> {
            commentService.deleteComment(commentId);
        });
    }

    @Test
    void addCommentToPost_shouldSaveCommentAndReturnDto() {
        // Given
        Long postId = 1L;

        CommentDto request = new CommentDto();
        request.setContent("This is a test comment");

        User mockUser = new User();
        mockUser.setUsername("testUser");
        mockUser.setEmail("user@example.com");

        Post mockPost = new Post();
        mockPost.setId(postId);
        mockPost.setTitle("Sample Post");

        Comment savedComment = new Comment();
        savedComment.setId(20L);
        savedComment.setContent("This is a test comment");
        savedComment.setAuthor("testUser");
        savedComment.setPost(mockPost);
        savedComment.setPost(mockPost);
        savedComment.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(postRepository.findById(postId)).thenReturn(Optional.of(mockPost));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        // Act
        CommentDto result = commentService.addCommentToPost(postId, request);

        // Assert
        assertNotNull(result);
        assertEquals("This is a test comment", result.getContent());

        // Then
        assertAll("Check CommentDto properties",
                () -> assertNotNull(result),
                () -> assertEquals(request.getContent(), result.getContent(), "Content should match"),
                () -> assertEquals(postId, result.getPostId(), "Post ID should match")
        );
    }

    @Test
    void addCommentToPost_NullContent() {
        // Given
        Long postId = 1L;
        CommentDto commentDto = new CommentDto();
        commentDto.setContent(null);

        // When and Then
        Exception exception = assertThrows(Exception.class, () ->
                commentService.addCommentToPost(postId, commentDto));

        assertInstanceOf(InvalidCommentException.class, exception);
    }

    @Test
    void testAddCommentToPost_UserNotFound_ThrowsException() {
        // Given
        Long postId = 1L;
        CommentDto commentDto = new CommentDto();
        commentDto.setContent("This is a great post!");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user@example.com");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Mock user not found
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        // When / Then
        assertThrows(UserAccountException.class, () -> commentService.addCommentToPost(postId, commentDto));
    }


    @Test
    void addCommentToPost_EmptyContent() {
        // Given
        Long postId = 1L;
        CommentDto commentDto = new CommentDto();
        commentDto.setContent(""); // Empty content

        // When and Then
        Exception exception = assertThrows(InvalidCommentException.class, () ->
                commentService.addCommentToPost(postId, commentDto));

        // Verify the exception message
        assertNotNull(exception);
    }

    @Test
    void addCommentToPost_InvalidPostId() {
        // Given
        Long postId = -1L; // Invalid post ID
        CommentDto commentDto = new CommentDto();
        commentDto.setContent("This is a great post!");

        // When and Then
        Exception exception = assertThrows(InvalidPostIdException.class, () ->
                commentService.addCommentToPost(postId, commentDto));

        assertEquals("Invalid post ID: " + postId, exception.getMessage());
    }

    @Test
    void updateComment_ShouldUpdateCommentSuccessfully() {
        Long postId = 1L;
        Long commentId = 10L;
        CommentDto requestDto = new CommentDto();
        requestDto.setContent("Updated content");

        Post mockPost = new Post();
        mockPost.setId(postId);

        Comment existingComment = new Comment();
        existingComment.setId(commentId);
        existingComment.setContent("Old content");
        existingComment.setPost(mockPost);

        when(postRepository.findById(postId)).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existingComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentDto result = commentService.updateComment(requestDto, commentId, postId);

        assertEquals("Updated content", result.getContent());
        verify(commentRepository).save(existingComment);
    }

    @Test
    void updateComment_ShouldThrowException_ForNonExistingPost() {
        Long postId = 99999L;
        Long commentId = 10L;
        CommentDto requestDto = new CommentDto();
        requestDto.setContent("Some content");

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        PostNotFoundException exception = assertThrows(
                PostNotFoundException.class,
                () -> commentService.updateComment(requestDto, commentId, postId)
        );

        assertEquals("Post not found with id: 99999", exception.getMessage());
    }

    @Test
    void updateComment_ShouldThrowException_ForNonExistingComment() {
        Long postId = 1L;
        Long commentId = 999999L;
        CommentDto requestDto = new CommentDto();
        requestDto.setContent("Some content");

        when(postRepository.findById(postId)).thenReturn(Optional.of(new Post()));
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        CommentNotFoundException exception = assertThrows(
                CommentNotFoundException.class,
                () -> commentService.updateComment(requestDto, commentId, postId)
        );

        assertEquals("Comment was not found", exception.getMessage());
    }
}
package com.codehacks.blog.service;

import com.codehacks.blog.post.exception.CommentAlreadyExistsException;
import com.codehacks.blog.post.exception.CommentNotFoundException;
import com.codehacks.blog.post.exception.InvalidCommentException;
import com.codehacks.blog.post.exception.InvalidPostIdException;
import com.codehacks.blog.post.exception.MissingAuthorException;
import com.codehacks.blog.post.model.Comment;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.post.repository.BlogRepository;
import com.codehacks.blog.post.repository.CommentRepository;
import com.codehacks.blog.post.dto.CommentDto;
import com.codehacks.blog.post.service.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private BlogRepository blogRepository;

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
                () -> assertEquals("John Doe", result.getAuthor(), "Author should match"),
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
    void testCreateComment() {
        // Given
        Long postId = 1L;
        Post post = new Post();
        post.setId(postId);

        CommentDto commentDto = new CommentDto();
        commentDto.setContent("This is a great post!");
        commentDto.setAuthor("John Doe");

        Comment expectedComment = new Comment();
        expectedComment.setContent(commentDto.getContent());
        expectedComment.setAuthor(commentDto.getAuthor());
        expectedComment.setPost(post);
        expectedComment.setCreatedAt(LocalDateTime.now());

        when(blogRepository.findById(anyLong())).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(expectedComment);

        // When
        CommentDto result = commentService.createComment(commentDto, postId);

        // Then
        assertAll("Check CommentDto properties",
                () -> assertNotNull(result),
                () -> assertEquals(commentDto.getContent(), result.getContent(), "Content should match"),
                () -> assertEquals(commentDto.getAuthor(), result.getAuthor(), "Author should match"),
                () -> assertEquals(postId, result.getPostId(), "Post ID should match")
        );
    }

    @Test
    void testCreateComment_DuplicateComment() {
        // Given
        Long postId = 1L;
        CommentDto commentDto = new CommentDto();
        commentDto.setContent("This is a great post!");
        commentDto.setAuthor("John Doe");

        when(commentRepository.existsByPostIdAndAuthorAndContent(postId, commentDto.getAuthor(), commentDto.getContent()))
                .thenReturn(true);

        // When and Then
        assertAll("Check for CommentAlreadyExistsException",
                () -> assertThrows(CommentAlreadyExistsException.class, () -> {
                    commentService.createComment(commentDto, postId);
                })
        );
    }

    @Test
    void testCreateComment_NullContent() {
        // Given
        Long postId = 1L;
        CommentDto commentDto = new CommentDto();
        commentDto.setAuthor("John Doe");
        commentDto.setContent(null);

        // When and Then
        Exception exception = assertThrows(Exception.class, () -> {
            commentService.createComment(commentDto, postId);
        });

        assertInstanceOf(InvalidCommentException.class, exception);
    }

    @Test
    void testCreateComment_NullAuthor() {
        // Given
        Long postId = 1L;
        CommentDto commentDto = new CommentDto();
        commentDto.setContent("This is a great post!");
        commentDto.setAuthor(null);

        // When and Then
        assertThrows(MissingAuthorException.class, () -> {
            commentService.createComment(commentDto, postId);
        });
    }

    @Test
    void testCreateComment_EmptyContent() {
        // Given
        Long postId = 1L;
        CommentDto commentDto = new CommentDto();
        commentDto.setAuthor("John Doe");
        commentDto.setContent(""); // Empty content

        // When and Then
        Exception exception = assertThrows(InvalidCommentException.class, () -> {
            commentService.createComment(commentDto, postId);
        });

        // Verify the exception message
        assertNotNull(exception);
    }

    @Test
    void testCreateComment_EmptyAuthor() {
        // Given
        Long postId = 1L;
        CommentDto commentDto = new CommentDto();
        commentDto.setContent("This is a great post!");
        commentDto.setAuthor("");

        when(commentRepository.existsByPostIdAndAuthorAndContent(postId, commentDto.getAuthor(), commentDto.getContent()))
                .thenReturn(false);

        // When and Then
        assertThrows(MissingAuthorException.class, () -> commentService.createComment(commentDto, postId));
    }

    @Test
    void testCreateComment_InvalidPostId() {
        // Given
        Long postId = -1L; // Invalid post ID
        CommentDto commentDto = new CommentDto();
        commentDto.setAuthor("John Doe");
        commentDto.setContent("This is a great post!");

        // When and Then
        Exception exception = assertThrows(InvalidPostIdException.class, () -> {
            commentService.createComment(commentDto, postId);
        });

        assertEquals("Invalid post ID: " + postId, exception.getMessage());
    }
}
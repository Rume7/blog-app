package com.codehacks.blog.post.service;

import com.codehacks.blog.post.dto.CommentDto;
import com.codehacks.blog.post.exception.*;
import com.codehacks.blog.post.model.Comment;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.post.repository.BlogRepository;
import com.codehacks.blog.post.repository.CommentRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final BlogRepository blogRepository;

    public CommentDto createComment(@Valid CommentDto commentDto, Long postId) {
        validateComment(commentDto, postId);

        Post post = blogRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + postId));

        Comment comment = new Comment();
        comment.setContent(commentDto.getContent());
        comment.setAuthor(commentDto.getAuthor());
        comment.setPost(post);
        comment.setCreatedAt(LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);
        return mapToDto(savedComment);
    }

    private void validateComment(@Valid CommentDto commentDto, Long postId) {
        if (postId <= 0) {
            throw new InvalidPostIdException("Invalid post ID: " + postId);
        }

        if (commentDto.getAuthor() == null || commentDto.getAuthor().isBlank()) {
            throw new MissingAuthorException("Comment's author missing");
        }

        if (commentDto.getContent() == null || commentDto.getContent().isEmpty()) {
            throw new InvalidCommentException("Comment's content is missing");
        }

        // Check if a comment with the same content and author already exists for the post
        boolean exists = commentRepository.existsByPostIdAndAuthorAndContent(postId,
                commentDto.getAuthor(),
                commentDto.getContent());

        if (exists) {
            throw new CommentAlreadyExistsException("Comment already exists for this post and author.");
        }
    }

    public CommentDto getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with ID: " + id));
        return mapToDto(comment);
    }

    public void deleteComment(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new CommentNotFoundException("Comment not found with ID: " + id);
        }
        commentRepository.deleteById(id);
    }

    public List<CommentDto> getAllCommentsForPost(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private CommentDto mapToDto(Comment comment) {
        CommentDto commentDto = new CommentDto();
        commentDto.setId(comment.getId());
        commentDto.setContent(comment.getContent());
        commentDto.setAuthor(comment.getAuthor());
        commentDto.setPostId(comment.getPost().getId());
        commentDto.setCreatedAt(comment.getCreatedAt());
        return commentDto;
    }
}

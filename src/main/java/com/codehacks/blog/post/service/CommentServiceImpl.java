package com.codehacks.blog.post.service;

import com.codehacks.blog.auth.exception.UserAccountException;
import com.codehacks.blog.auth.model.User;
import com.codehacks.blog.auth.repository.UserRepository;
import com.codehacks.blog.post.dto.CommentDto;
import com.codehacks.blog.post.exception.CommentNotFoundException;
import com.codehacks.blog.post.exception.InvalidCommentException;
import com.codehacks.blog.post.exception.InvalidPostIdException;
import com.codehacks.blog.post.exception.PostNotFoundException;
import com.codehacks.blog.post.model.Comment;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.post.repository.BlogRepository;
import com.codehacks.blog.post.repository.CommentRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final BlogRepository blogRepository;

    public CommentDto addCommentToPost(Long postId, CommentDto request) {
        validateComment(request, postId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UserAccountException("User not found"));

        Post post = blogRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setAuthor(user.getUsername());
        comment.setPost(post);

        Comment savedComment = commentRepository.save(comment);
        return mapToDto(savedComment);
    }

    private void validateComment(@Valid CommentDto commentDto, Long postId) {
        if (postId <= 0) {
            throw new InvalidPostIdException("Invalid post ID: " + postId);
        }

        if (commentDto.getContent() == null || commentDto.getContent().isEmpty()) {
            throw new InvalidCommentException("Comment's content is missing");
        }
    }

    public CommentDto updateComment(@Valid CommentDto commentDto, Long commentId, Long postId) {
        validateComment(commentDto, postId);

        blogRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + postId));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment was not found"));

        comment.setContent(commentDto.getContent());
        comment.setUpdatedAt(LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);
        return mapToDto(savedComment);
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
        commentDto.setPostId(comment.getPost().getId());
        commentDto.setCreatedAt(comment.getCreatedAt());
        return commentDto;
    }
}

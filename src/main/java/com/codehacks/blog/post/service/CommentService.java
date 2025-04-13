package com.codehacks.blog.post.service;

import java.util.List;

import com.codehacks.blog.post.dto.CommentDto;
import jakarta.validation.Valid;

public interface CommentService {

    CommentDto addCommentToPost(Long postId, @Valid CommentDto commentDto);

    CommentDto updateComment(@Valid CommentDto commentDto, Long commentId, Long postId);

    CommentDto getCommentById(Long id);

    void deleteComment(Long id);

    List<CommentDto> getAllCommentsForPost(Long postId);
}

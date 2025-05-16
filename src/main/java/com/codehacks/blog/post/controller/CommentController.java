package com.codehacks.blog.post.controller;

import com.codehacks.blog.post.dto.CommentDto;
import com.codehacks.blog.post.service.CommentService;
import com.codehacks.blog.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Constants.COMMENT_PATH)
@Tag(name = "Post Comment Management", description = "APIs for managing the comments associated with posts")
public class CommentController {

    @Autowired
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "Add comment to post",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "201", description = "Comment added successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400", description = "Invalid input")
            })
    @PostMapping(value = "/{postId}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommentDto> addComment(@PathVariable Long postId, @RequestBody @Valid CommentDto commentRequest) {
        CommentDto createdComment = commentService.addCommentToPost(postId, commentRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }


    @Operation(summary = "Update a comment",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "Comment updated successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404", description = "Comment not found")
            })
    @PutMapping(value = "/update/{postId}/{commentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommentDto> updateComment(@PathVariable Long postId, @PathVariable Long commentId,
                                                    @RequestBody CommentDto commentDto) {
        CommentDto createdComment = commentService.updateComment(commentDto, commentId, postId);
        return ResponseEntity.status(HttpStatus.OK).body(createdComment);
    }


    @Operation(summary = "Get all comments for a post",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "List of comments returned")
            })
    @GetMapping(value = "/post/{postId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CommentDto>> getAllCommentsForPost(@PathVariable Long postId) {
        List<CommentDto> comments = commentService.getAllCommentsForPost(postId);
        return ResponseEntity.ok(comments);
    }


    @Operation(summary = "Get comment by ID",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200", description = "Comment found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404", description = "Comment not found")
            })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommentDto> getCommentById(@PathVariable Long id) {
        CommentDto comment = commentService.getCommentById(id);
        return ResponseEntity.ok(comment);
    }


    @Operation(summary = "Delete a comment",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "204", description = "Comment deleted"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404", description = "Comment not found")
            })
    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}

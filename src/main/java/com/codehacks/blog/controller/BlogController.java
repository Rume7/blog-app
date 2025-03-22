package com.codehacks.blog.controller;

import com.codehacks.blog.dto.ApiResponse;
import com.codehacks.blog.dto.BlogPreviewDTO;
import com.codehacks.blog.exception.InvalidPostException;
import com.codehacks.blog.model.Post;
import com.codehacks.blog.service.BlogService;
import com.codehacks.blog.util.Constants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
import java.util.Set;

@Slf4j
@RestController
@RequestMapping(Constants.BLOG_PATH)
@AllArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @GetMapping(value = "/all", produces = "application/json")
    public ResponseEntity<Set<Post>> getAllPosts() {
        Set<Post> allPosts = blogService.getAllPosts();
        return ResponseEntity.ok(allPosts);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        Post post = blogService.getPostById(id);
        if (post != null) {
            return new ResponseEntity<>(post, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping(value = "/create", produces = "application/json")
    public ResponseEntity<ApiResponse<Post>> createPost(@Valid @RequestBody Post post) throws InvalidPostException {
        log.info("Received Post: {}", post);
        Post createdPost = blogService.createPost(post);
        return ResponseEntity.ok(ApiResponse.created(createdPost));
    }

    @PutMapping(value = "/update/{id}", produces = "application/json")
    public ResponseEntity<ApiResponse<Post>> updatePost(@PathVariable @Positive Long id, @Valid @RequestBody Post post) {
        log.info("Updated Post: {}", post);
        Post updatedPost = blogService.updatePost(post, id);
        if (updatedPost == null) {
            return ResponseEntity.ok(ApiResponse.error(Constants.POST_NOT_FOUND + id));
        }
        return ResponseEntity.ok(ApiResponse.success(updatedPost));
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) throws InvalidPostException {
        boolean deletePost = blogService.deletePost(id);
        if (deletePost) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/previews")
    public ResponseEntity<List<BlogPreviewDTO>> getBlogPreviews() {
        List<BlogPreviewDTO> previews = blogService.getBlogPreviews();
        return ResponseEntity.ok(previews);
    }
}
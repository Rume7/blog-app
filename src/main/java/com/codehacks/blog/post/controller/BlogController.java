package com.codehacks.blog.post.controller;

import com.codehacks.blog.auth.dto.ApiResponse;
import com.codehacks.blog.post.dto.BlogPreviewDTO;
import com.codehacks.blog.auth.exception.InvalidPostException;
import com.codehacks.blog.post.dto.PostSummaryDTO;
import com.codehacks.blog.post.model.Author;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.post.service.BlogService;
import com.codehacks.blog.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping(Constants.BLOG_PATH)
public class BlogController {

    private final BlogService blogService;
    private final int defaultRecentLimit;

    public BlogController(BlogService blogService, @Value("${blog.recent.limit}") int defaultRecentLimit) {
        this.blogService = blogService;
        this.defaultRecentLimit = defaultRecentLimit;
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<Post>> getAllPosts() {
        Set<Post> allPosts = blogService.getAllPosts();
        return ResponseEntity.ok(allPosts);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Post> getPostById(@Valid @Positive @PathVariable Long id) {
        Post post = blogService.getPostById(id);
        if (post != null) {
            return new ResponseEntity<>(post, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    @Operation(summary = "Create a new blog post",
            description = "Only accessible by ADMIN or AUTHOR",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Post>> createPost(@Valid @RequestBody Post post) throws InvalidPostException {
        log.info("Received Post: {}", post);
        try {
            Post createdPost = blogService.createPost(post);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(createdPost));
        } catch (InvalidPostException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PutMapping(value = "/update/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Post>> updatePost(@PathVariable @Positive Long id, @Valid @RequestBody Post post) {
        log.info("Updated Post: {}", post);
        Post updatedPost = blogService.updatePost(post, id);
        if (updatedPost == null) {
            return ResponseEntity.ok(ApiResponse.error(Constants.POST_NOT_FOUND + id));
        }
        return ResponseEntity.ok(ApiResponse.success(updatedPost));
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deletePost(@PathVariable Long id) throws InvalidPostException {
        boolean deletePost = blogService.deletePost(id);
        if (deletePost) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/previews", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BlogPreviewDTO>> getBlogPreviews() {
        List<BlogPreviewDTO> previews = blogService.getBlogPreviews();
        return ResponseEntity.ok(previews);
    }

    @GetMapping(value = "/recent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PostSummaryDTO>> getRecentPosts(
            @RequestParam(value = "limit", required = false) Integer limit) {
        if (limit != null && limit <= 0) {
            return ResponseEntity.badRequest().build();
        }
        int safeLimit = (limit != null) ? Math.min(limit, 10) : defaultRecentLimit;
        Pageable pageable = PageRequest.of(0, safeLimit);
        List<PostSummaryDTO> posts = blogService.getRecentPosts(pageable);
        return ResponseEntity.ok(posts);
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PostSummaryDTO>> searchPosts(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "true") boolean caseSensitive,
            @RequestParam(required = false, defaultValue = "true") boolean exactMatch) {
        if (query == null || query.trim().isEmpty()) {
            throw new InvalidPostException("Search query cannot be empty");
        }
        List<PostSummaryDTO> results = blogService.searchPosts(query.trim(), caseSensitive, exactMatch);
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/search/author", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Post>> getPostsByAuthor(@RequestParam String firstName, String lastName, String email) {
        if (firstName == null || firstName.trim().isEmpty()
                || lastName == null || lastName.trim().isEmpty()
                || email == null || email.trim().isEmpty()) {
            throw new InvalidPostException("Author name cannot be empty");
        }
        Author author = new Author(firstName, lastName, email);
        List<Post> posts = blogService.getPostsByAuthor(author);
        return ResponseEntity.ok(posts);
    }
}
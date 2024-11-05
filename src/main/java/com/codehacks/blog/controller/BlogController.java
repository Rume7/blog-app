package com.codehacks.blog.controller;

import com.codehacks.blog.dto.PostDTO;
import com.codehacks.blog.exception.InvalidPostException;
import com.codehacks.blog.mapper.PostMapper;
import com.codehacks.blog.model.Post;
import com.codehacks.blog.service.BlogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/blog")
@AllArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final PostMapper postMapper;

    @GetMapping(value = "/all", produces = "application/json")
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        Set<Post> allPosts = blogService.getAllPosts();
        List<PostDTO> postDTOs = allPosts.stream().map(postMapper::toDto).toList();
        return ResponseEntity.ok(postDTOs);
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
    public ResponseEntity<Post> createPost(@Valid @RequestBody PostDTO postDTO) throws InvalidPostException {
        Post post = postMapper.toEntity(postDTO);
        Post createdPost = blogService.createPost(post);
        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update/{id}", produces = "application/json")
    public ResponseEntity<Post> updatePost(@PathVariable @Positive Long id,
                                           @Valid @RequestBody Post post) {
        Post updatedPost = blogService.updatePost(post, id);
        return updatedPost != null ?
                ResponseEntity.ok(updatedPost) :
                ResponseEntity.notFound().build();
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        boolean deletePost = blogService.deletePost(id);
        if (deletePost) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

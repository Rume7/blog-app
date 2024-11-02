package com.codehacks.blog.controller;

import com.codehacks.blog.model.Post;
import com.codehacks.blog.service.BlogService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/posts")
@AllArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @GetMapping(value = "/all", produces = "application/json")
    public ResponseEntity<Set<Post>> getAllPosts() {
        Set<Post> posts = blogService.getAllPosts();
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        Post post = blogService.getAPost(id);
        if (post != null) {
            return new ResponseEntity<>(post, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping(value = "/create", produces = "application/json")
    public ResponseEntity<Post> createPost(@RequestBody Post post) {
        Post createdPost = blogService.createPost(post);
        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update/{id}", produces = "application/json")
    public ResponseEntity<Post> updatePost(@PathVariable Long id, @RequestBody Post post) {
        Post updatedPost = blogService.updatePost(post, id);
        if (updatedPost == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(updatedPost, HttpStatus.OK);
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

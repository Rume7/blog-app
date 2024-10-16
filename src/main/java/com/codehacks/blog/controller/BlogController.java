package com.codehacks.blog.controller;

import com.codehacks.blog.model.Post;
import com.codehacks.blog.service.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class BlogController {

    @Autowired
    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    public ResponseEntity<List<Post>> getPosts() {
        List<Post> posts = blogService.getPosts();
        return ResponseEntity.ok(posts);
    }

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Post post) {
        Post createdPost = blogService.createPost(post);
        if (createdPost == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(createdPost);
    }
}

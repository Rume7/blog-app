package com.codehacks.blog.controller;

import com.codehacks.blog.model.Post;
import com.codehacks.blog.service.BlogServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * A controller for the blog application.
 */
@RestController
@RequestMapping("/api/posts")
public class BlogController {

    private final BlogServiceImpl blogServiceImpl;

    @Autowired
    public BlogController(final BlogServiceImpl blogServiceImpl) {
        this.blogServiceImpl = blogServiceImpl;
    }

    @GetMapping
    public ResponseEntity<List<Post>> getPosts() {
        List<Post> posts = blogServiceImpl.getPosts();
        return ResponseEntity.ok(posts);
    }

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Post post) {
        Post createdPost = blogServiceImpl.createPost(post);
        if (createdPost == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(createdPost);
    }
}

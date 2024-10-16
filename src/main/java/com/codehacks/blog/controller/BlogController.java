package com.codehacks.blog.controller;

import com.codehacks.blog.model.Post;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class BlogController {
    private List<Post> posts = new ArrayList<>();

    @GetMapping
    public List<Post> getPosts() {
        return posts;
    }

    @PostMapping
    public Post createPost(@RequestBody Post post) {
        posts.add(post);
        return post;
    }
}

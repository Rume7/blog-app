package com.codehacks.blog.service;

import com.codehacks.blog.model.Post;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

@Service
public class BlogServiceImpl {

    private List<Post> posts = new ArrayList<>();

    public List<Post> getPosts() {
        return posts;
    }

    public Post createPost(@RequestBody final Post post) {
        posts.add(post);
        return post;
    }
}

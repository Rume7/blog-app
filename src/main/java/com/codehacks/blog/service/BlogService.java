package com.codehacks.blog.service;

import com.codehacks.blog.model.Post;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface BlogService {

    public List<Post> getPosts() ;

    public Post createPost(@RequestBody final Post post);
}

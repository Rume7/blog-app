package com.codehacks.blog.service;

import com.codehacks.blog.model.Post;

import java.util.Set;

public interface BlogService {

    Set<Post> getAllPosts();

    Post getAPost(Long blogId);

    Post createPost(final Post post);

    Post updatePost(final Post post, Long blogId);

    Boolean deletePost(Long blogId);
}

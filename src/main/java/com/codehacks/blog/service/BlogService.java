package com.codehacks.blog.service;

import com.codehacks.blog.exception.InvalidPostException;
import com.codehacks.blog.model.Author;
import com.codehacks.blog.model.Post;

import java.util.List;
import java.util.Set;

public interface BlogService {

    Set<Post> getAllPosts();

    Post getPostById(Long blogId);

    List<Post> searchPostsByTitle(String title);

    List<Post> getPostsByAuthor(Author authorName);

    Post createPost(final Post post) throws InvalidPostException;

    Post updatePost(final Post post, Long blogId);

    Boolean deletePost(Long blogId);
}

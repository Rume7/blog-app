package com.codehacks.blog.post.service;

import com.codehacks.blog.post.dto.BlogPreviewDTO;
import com.codehacks.blog.auth.exception.InvalidPostException;
import com.codehacks.blog.post.dto.PostSummaryDTO;
import com.codehacks.blog.post.model.Author;
import com.codehacks.blog.post.model.Post;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface BlogService {

    Set<Post> getAllPosts();

    Post getPostById(Long blogId);

    List<Post> searchPostsByTitle(String title);

    List<Post> getPostsByAuthor(Author authorName);

    Post createPost(final Post post) throws InvalidPostException;

    Post updatePost(final Post post, Long blogId);

    Boolean deletePost(Long blogId) throws InvalidPostException;

    List<BlogPreviewDTO> getBlogPreviews();

    List<PostSummaryDTO> getRecentPosts(Pageable pageable);

    Set<PostSummaryDTO> searchPosts(String query, boolean caseSensitive, boolean exactMatch);
}

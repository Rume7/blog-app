package com.codehacks.blog.service;

import com.codehacks.blog.exception.PostNotFoundException;
import com.codehacks.blog.model.Post;
import com.codehacks.blog.repository.BlogRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;

    @Autowired
    public BlogServiceImpl(BlogRepository blogRepository) {
        this.blogRepository = blogRepository;
    }

    @Override
    public Set<Post> getAllPosts() {
        List<Post> postList = blogRepository.findAll();
        return postList.isEmpty() ? null
                : new HashSet<>(blogRepository
                    .findAll());
    }

    @Override
    public Post getAPost(Long id) {
        if (id == null || id < 1) {
            throw new PostNotFoundException("Blog id " + id + " is invalid");
        }
        return blogRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post " + id + " was not found"));
    }

    @Override
    public Post createPost(Post post) {
        return blogRepository.save(post);
    }

    @Override
    public Post updatePost(Post post, Long blogId) {
        if (blogRepository.existsById(blogId) && post != null) {
            Post blogPost = blogRepository.findById(blogId).get();
            blogPost.setTitle(post.getTitle());
            blogPost.setContent(post.getContent());
            blogPost.setUpdatedAt(LocalDateTime.now());
            return blogRepository.save(blogPost);
        }
        return null;
    }

    @Override
    public Boolean deletePost(Long blogId) {
        if (blogRepository.existsById(blogId)) {
            blogRepository.deleteById(blogId);
            return true;
        }
        return false;
    }
}

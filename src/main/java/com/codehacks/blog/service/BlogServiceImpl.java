package com.codehacks.blog.service;

import com.codehacks.blog.model.Post;
import com.codehacks.blog.repository.BlogRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
public class BlogServiceImpl implements BlogService {

    @Autowired
    private final BlogRepository blogRepository;

    @Override
    public Set<Post> getAllPosts() {
        return blogRepository
                .findAll()
                .stream()
                .collect(Collectors.toSet());
    }

    @Override
    public Post getAPost(Long id) {
        Optional<Post> post = blogRepository.findById(id);
        if (post.isPresent()) {
            return post.get();
        }
        throw new RuntimeException("Post not found");
    }

    @Override
    public Post createPost(Post post) {
        return null;
    }

    @Override
    public Post updatePost(Post post, Long blogId) {
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

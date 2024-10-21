package com.codehacks.blog.service;

import com.codehacks.blog.model.Post;
import com.codehacks.blog.repository.BlogRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
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
        return null;
    }

    @Override
    public Post createPost(Post post) {
        Post savedPost = blogRepository.save(post);
        return savedPost;
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

package com.codehacks.blog.service;

import com.codehacks.blog.exception.InvalidPostException;
import com.codehacks.blog.exception.PostNotFoundException;
import com.codehacks.blog.model.Author;
import com.codehacks.blog.model.Post;
import com.codehacks.blog.repository.AuthorRepository;
import com.codehacks.blog.repository.BlogRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final AuthorRepository authorRepository;

    @Override
    public Set<Post> getAllPosts() {
        return new HashSet<>(blogRepository.findAll());
    }

    @Override
    public Post getPostById(Long id) {
        if (id == null || id < 1) {
            throw new PostNotFoundException("Blog id " + id + " is invalid");
        }
        return blogRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post " + id + " was not found"));
    }

    @Override
    public List<Post> searchPostsByTitle(String title) {
        return blogRepository.findByTitleContainingIgnoreCase(title);
    }

    @Override
    public List<Post> getPostsByAuthor(Author authorName) {
        return blogRepository.findByAuthor(authorName);
    }

    @Override
    public Post createPost(Post post) throws InvalidPostException {
        if (post.getTitle() == null ||
            post.getTitle().trim().length() < 8 ||
            post.getTitle().length() > 100) {
            throw new InvalidPostException("Post title must be between 8 and 100 characters");
        }
        Author savedAuthor = authorRepository.save(post.getAuthor());
        post.setAuthor(savedAuthor);
        return blogRepository.save(post);
    }

    @Override
    public Post updatePost(Post post, Long blogId) {
        if (blogRepository.existsById(blogId) && post != null) {
            Optional<Post> retrievedPost = blogRepository.findById(blogId);
            if (retrievedPost.isPresent()) {
                Post blogPost = retrievedPost.get();
                blogPost.setTitle(post.getTitle());
                blogPost.setContent(post.getContent());
                blogPost.setUpdatedAt(LocalDateTime.now());
                return blogRepository.save(blogPost);
            }
        }
        throw new PostNotFoundException("Post not found with id: " + blogId);
    }

    @Override
    public Boolean deletePost(Long blogId) {
        if (blogRepository.existsById(blogId)) {
            blogRepository.deleteById(blogId);
            return true;
        }
        throw new PostNotFoundException("Post not found with id: " + blogId);
    }
}

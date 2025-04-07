package com.codehacks.blog.service;

import com.codehacks.blog.dto.BlogPreviewDTO;
import com.codehacks.blog.exception.InvalidPostException;
import com.codehacks.blog.exception.PostNotFoundException;
import com.codehacks.blog.model.Author;
import com.codehacks.blog.model.Post;
import com.codehacks.blog.repository.AuthorRepository;
import com.codehacks.blog.repository.BlogRepository;
import com.codehacks.blog.util.Constants;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Transactional
    public Post createPost(Post post) throws InvalidPostException {
        if (post.getTitle() == null) {
            throw new InvalidPostException("Title cannot be null");
        }
        if (post.getTitle().trim().length() < Constants.MIN_TITLE_LENGTH) {
            throw new InvalidPostException("Title length is too short");
        }
        if (post.getTitle().trim().length() > Constants.MAX_TITLE_LENGTH) {
            throw new InvalidPostException("Title length is too long");
        }
        Author savedAuthor = authorRepository.save(post.getAuthor());
        post.setAuthor(savedAuthor);
        return blogRepository.save(post);
    }

    @Override
    @Transactional
    public Post updatePost(Post post, Long blogId) {
        Post blogPost = blogRepository.findById(blogId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + blogId));

        blogPost.setTitle(post.getTitle());
        blogPost.setContent(post.getContent());
        blogPost.setUpdatedAt(LocalDateTime.now());

        return blogRepository.save(blogPost);
    }

    @Override
    @Transactional
    public Boolean deletePost(Long blogId) throws InvalidPostException {
        if (blogId <= 0) {
            throw new InvalidPostException("Post cannot have a non-positive id");
        }
        if (blogRepository.existsById(blogId)) {
            blogRepository.deleteById(blogId);
            return true;
        }
        throw new PostNotFoundException("Post not found with id: " + blogId);
    }

    @Override
    public List<BlogPreviewDTO> getBlogPreviews() {
        List<Post> blogs = blogRepository.findAll();
        return blogs.stream()
                .map(this::convertToPreview)
                .collect(Collectors.toList());
    }

    private BlogPreviewDTO convertToPreview(Post blog) {
        BlogPreviewDTO preview = new BlogPreviewDTO();
        preview.setId(blog.getId());
        preview.setTitle(blog.getTitle());
        preview.setAuthor(String.join(" ", blog.getAuthor().getFirstName(), blog.getAuthor().getLastName()));
        preview.setCreatedAt(blog.getCreatedAt());

        // Get first two paragraphs
        String[] paragraphs = blog.getContent().split("\n\n");
        String previewContent = Arrays.stream(paragraphs)
                .limit(2)
                .collect(Collectors.joining("\n\n"));
        preview.setPreviewContent(previewContent);

        return preview;
    }
}

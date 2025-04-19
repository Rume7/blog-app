package com.codehacks.blog.post.service;

import com.codehacks.blog.post.dto.BlogPreviewDTO;
import com.codehacks.blog.auth.exception.InvalidPostException;
import com.codehacks.blog.post.dto.PostSummaryDTO;
import com.codehacks.blog.post.exception.PostNotFoundException;
import com.codehacks.blog.post.mapper.PostMapper;
import com.codehacks.blog.post.model.Author;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.post.repository.AuthorRepository;
import com.codehacks.blog.post.repository.BlogRepository;
import com.codehacks.blog.util.Constants;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
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
    private final PostMapper postMapper;

    @Value("${blog.recent.limit}")

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
        if (post.getTitle() == null || post.getTitle().trim().isEmpty()) {
            throw new InvalidPostException("Title cannot be null or empty or only whitespace");
        }
        if (post.getTitle().trim().length() < Constants.MIN_TITLE_LENGTH) {
            throw new InvalidPostException("Title length is too short");
        }
        if (post.getTitle().trim().length() > Constants.MAX_TITLE_LENGTH) {
            throw new InvalidPostException("Title length is too long");
        }

        post.setTitle(post.getTitle().trim());

        Author existingAuthor = authorRepository.findByEmail(post.getAuthor().getEmail());
        if (existingAuthor != null) {
            post.setAuthor(existingAuthor);
        } else {
            Author savedAuthor = authorRepository.save(post.getAuthor());
            post.setAuthor(savedAuthor);
        }

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

        String content = blog.getContent();
        int wordCount = countWords(content);

        String previewContent;
        final int numberOfWords = 600;
        if (wordCount >= numberOfWords) {
            int previewWordCount = (int) (wordCount * 0.8);
            previewContent = getFirstNWords(content, previewWordCount);
        } else {
            // Get first two paragraphs
            String[] paragraphs = blog.getContent().split("\n\n");
            previewContent = Arrays.stream(paragraphs)
                    .limit(2)
                    .collect(Collectors.joining("\n\n"));
        }
        preview.setPreviewContent(previewContent);

        return preview;
    }

    private int countWords(String content) {
        return content.trim().split("\\s+").length;
    }

    private String getFirstNWords(String content, int n) {
        String[] words = content.trim().split("\\s+");
        return String.join(" ", Arrays.copyOfRange(words, 0, Math.min(n, words.length)));
    }

    public List<PostSummaryDTO> getRecentPosts(Pageable pageable) {
        return blogRepository.findTopNRecentPostsOrderByCreatedAt(pageable)
                .stream()
                .map(postMapper::toSummary)
                .toList();
    }
}

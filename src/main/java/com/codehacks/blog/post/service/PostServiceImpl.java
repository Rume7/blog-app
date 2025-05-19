package com.codehacks.blog.post.service;

import com.codehacks.blog.auth.exception.InvalidSearchQueryException;
import com.codehacks.blog.post.dto.PostPreviewDTO;
import com.codehacks.blog.auth.exception.InvalidPostException;
import com.codehacks.blog.post.dto.PostSummaryDTO;
import com.codehacks.blog.post.exception.MissingAuthorException;
import com.codehacks.blog.post.exception.PostNotFoundException;
import com.codehacks.blog.post.mapper.PostMapper;
import com.codehacks.blog.post.model.Author;
import com.codehacks.blog.post.model.Post;
import com.codehacks.blog.post.repository.AuthorRepository;
import com.codehacks.blog.post.repository.PostRepository;
import com.codehacks.blog.util.Constants;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final AuthorRepository authorRepository;
    private final PostMapper postMapper;

    @Override
    public Set<Post> getAllPosts() {
        return new HashSet<>(postRepository.findAll());
    }

    @Override
    @Cacheable(value = "posts", key = "#postId")
    public Post getPostById(Long id) {
        if (id == null || id < 1) {
            throw new PostNotFoundException("Post id: " + id + " is invalid");
        }
        return postRepository.findByIdWithComments(id)
                .orElseThrow(() -> new PostNotFoundException("Post " + id + " was not found"));
    }

    @Override
    public List<Post> searchPostsByTitle(String title) {
        return postRepository.findByTitleContainingIgnoreCase(title);
    }

    @Override
    public List<Post> getPostsByAuthor(Author authorName) {
        validateAuthorName(authorName);

        return postRepository.findByAuthor(authorName);
    }

    private void validateAuthorName(Author author) {
        if (author == null) {
            throw new MissingAuthorException("Author can't be null");
        }
        String firstName = author.getFirstName();
        String lastName = author.getLastName();
        String emailAddress = author.getEmail();

        if (Stream.of(firstName, lastName, emailAddress)
                .map(s -> Objects.requireNonNullElse(s, ""))
                .anyMatch(String::isBlank)) {
            throw new MissingAuthorException("Author name cannot be empty");
        }
    }

    @Override
    @Transactional
    public Post createPost(Post post) {
        validatePost(post);

        post.setTitle(post.getTitle().trim());
        post.setContent(post.getContent().trim());

        Author author = resolveAuthor(post.getAuthor());
        post.setAuthor(author);

        return postRepository.save(post);
    }

    private void validatePost(Post post) {
        Objects.requireNonNull(post, "Post cannot be null");

        String title = Optional.ofNullable(post.getTitle())
                .map(String::trim)
                .orElseThrow(() -> new InvalidPostException("Title cannot be null or empty"));

        if (title.length() < Constants.MIN_TITLE_LENGTH) {
            throw new InvalidPostException("Title is too short");
        }

        if (title.length() > Constants.MAX_TITLE_LENGTH) {
            throw new InvalidPostException("Title is too long");
        }

        String content = Optional.ofNullable(post.getContent())
                .map(String::trim)
                .orElseThrow(() -> new InvalidPostException("Content cannot be null nor empty"));

        if (content.length() < 10) {
            throw new InvalidPostException("Content is too short");
        }

        if (content.length() > Constants.MAX_CONTENT_LENGTH) {
            throw new InvalidPostException("Content is too long");
        }

        Author author = post.getAuthor();
        if (author == null
                || author.getFirstName().isBlank()
                || author.getLastName().isBlank()
                || author.getEmail() == null
                || author.getEmail().isBlank()) {
            throw new InvalidPostException("Valid author details are required");
        }
    }

    private Author resolveAuthor(Author author) {
        return Optional.ofNullable(authorRepository.findByEmail(author.getEmail()))
                .orElseGet(() -> authorRepository.save(author));
    }

    @Override
    @Transactional
    public Post updatePost(Post post, Long blogId) {
        Post blogPost = postRepository.findByIdWithComments(blogId)
                .orElseThrow(() -> new PostNotFoundException("Post not found with id: " + blogId));

        validatePost(post);

        blogPost.setTitle(post.getTitle().trim());
        blogPost.setContent(post.getContent().trim());
        blogPost.setUpdatedAt(LocalDateTime.now());

        return postRepository.save(blogPost);
    }

    @Override
    @Transactional
    public Boolean deletePost(Long blogId) throws InvalidPostException {
        if (blogId <= 0) {
            throw new InvalidPostException("Post cannot have a non-positive id");
        }
        if (postRepository.existsById(blogId)) {
            postRepository.deleteById(blogId);
            return true;
        }
        throw new PostNotFoundException("Post not found with id: " + blogId);
    }

    @Override
    @Cacheable
    public List<PostPreviewDTO> getBlogPreviews() {
        List<Post> blogs = postRepository.findAll();
        return blogs.stream()
                .map(this::convertToPreview)
                .collect(Collectors.toList());
    }

    private PostPreviewDTO convertToPreview(Post blog) {
        PostPreviewDTO preview = new PostPreviewDTO();
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

    @Cacheable
    public List<PostSummaryDTO> getRecentPosts(Pageable pageable) {
        if (pageable == null) {
            throw new PostNotFoundException("There should be a number of posts.");
        }

        return postRepository.findTopNRecentPostsOrderByCreatedAt(pageable)
                .stream()
                .map(postMapper::toSummary)
                .toList();
    }

    @Override
    public Set<PostSummaryDTO> searchPosts(String query, boolean caseSensitive, boolean exactMatch) {
        if (query == null || query.isBlank()) {
            throw new InvalidSearchQueryException("Search query cannot be empty");
        }

        final String searchQuery = (!caseSensitive) ? query.trim().toLowerCase()
                : query.trim();

        List<Post> posts = postRepository.findAll();
        return posts.stream()
                .filter(post -> matchesSearchCriteria(post, searchQuery, caseSensitive, exactMatch))
                .map(postMapper::toSummary)
                .collect(Collectors.toSet());
    }

    private boolean matchesSearchCriteria(Post post, String searchQuery, boolean caseSensitive, boolean exactMatch) {
        String title = caseSensitive ? post.getTitle() : post.getTitle().toLowerCase();
        String content = caseSensitive ? post.getContent() : post.getContent().toLowerCase();

        if (exactMatch) {
            return title.equals(searchQuery) || content.equals(searchQuery);
        } else {
            return title.contains(searchQuery) || content.contains(searchQuery);
        }
    }
}

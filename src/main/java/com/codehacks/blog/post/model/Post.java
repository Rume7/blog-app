package com.codehacks.blog.post.model;

import com.codehacks.blog.util.Constants;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_slug", columnList = "slug", unique = true),
        @Index(name = "idx_posts_status", columnList = "status"),
        @Index(name = "idx_posts_author", columnList = "author_id"),
        @Index(name = "idx_posts_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title cannot be empty")
    @Size(min = Constants.MIN_TITLE_LENGTH, max = Constants.MAX_TITLE_LENGTH,
            message = "Title must be between " + Constants.MIN_TITLE_LENGTH + " and " + Constants.MAX_TITLE_LENGTH + " characters")
    private String title;

    @NotBlank(message = "Blog post cannot be empty")
    @Column(length = Constants.MAX_CONTENT_LENGTH)
    private String content;

    @NotBlank(message = "Slug cannot be empty")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Column(unique = true)
    private String slug;

    @Column(name = "featured_image_url")
    private String featuredImageUrl;

    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @JsonManagedReference
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    private List<Comment> comments = new ArrayList<>();

    public Post(String title, String content, Author author) {
        setTitle(title);
        setContent(content);
        setAuthor(author);
        this.slug = generateSlug(title);
    }

    public Post(String title, String content, Author author, LocalDateTime createdAt) {
        setTitle(title);
        setContent(content);
        setAuthor(author);
        this.createdAt = createdAt;
        this.slug = generateSlug(title);
    }

    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
        if (title != null) {
            this.slug = generateSlug(title);
        }
    }

    public void setContent(String content) {
        this.content = content != null ? content.trim() : null;
    }

    public void setStatus(Status status) {
        if (status == Status.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
        this.status = status;
    }

    private String generateSlug(String title) {
        if (title == null) return null;
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }
}

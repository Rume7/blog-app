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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Blog post cannot be empty")
    @Column(length = Constants.MAX_CONTENT_LENGTH)
    private String content;

    @Column
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    private Author author;

    @JsonManagedReference
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    public Post(String title, String content, Author author) {
        this.title = title != null ? title.trim() : null;
        this.content = content != null ? content.trim() : null;
        this.author = new Author(author.getFirstName(), author.getLastName());
    }

    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
    }

    public void setContent(String content) {
        this.content = content != null ? content.trim() : null;
    }
}

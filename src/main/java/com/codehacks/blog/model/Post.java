package com.codehacks.blog.model;

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

    @OneToOne(cascade = CascadeType.ALL)
    private Author author;

    @JsonManagedReference
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PostComment> allComments;

    public Post(String title, String content) {
        this.title = title != null ? title.trim() : null;
        this.content = content != null ? content.trim() : null;
        this.author = new Author();
        this.allComments = new ArrayList<>();
    }

    public Post(String title, String content, Author author) {
        this(title, content);
        this.author.setFirstName(author.getFirstName());
        this.author.setLastName(author.getLastName());
    }

    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
    }

    public void setContent(String content) {
        this.content = content != null ? content.trim() : null;
    }
}

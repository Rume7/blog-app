package com.codehacks.blog.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "BlogPosts")
@Data
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Blog post cannot be empty")
    @Column(length = 10000)
    private String content;

    @Column
    @CreatedDate
    private LocalDateTime createdAt;

    @Column
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(cascade = CascadeType.ALL)
    private Author author;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PostComment> allComments;

    public Post(String title, String content) {
        this.title = title != null ? title.trim() : null;
        this.content = content != null ? content.trim() : null;
        this.author = new Author();
        this.allComments = new ArrayList<>();
    }

    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
    }

    public void setContent(String content) {
        this.content = content != null ? content.trim() : null;
    }
}

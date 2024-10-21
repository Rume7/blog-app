package com.codehacks.blog.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table
@Data
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;

    @Column
    private String content;

    @Column
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column
    @CreationTimestamp
    private LocalDateTime updatedAt;

    public Post(String title, String content) {
        this.title = title;
        this.content = content;
    }
}

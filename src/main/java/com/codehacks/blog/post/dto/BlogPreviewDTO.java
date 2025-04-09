package com.codehacks.blog.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BlogPreviewDTO {

    private Long id;
    private String title;
    private String author;
    private String previewContent;
    private LocalDateTime createdAt;
}

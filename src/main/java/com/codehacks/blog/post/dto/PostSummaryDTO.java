package com.codehacks.blog.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class PostSummaryDTO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
}

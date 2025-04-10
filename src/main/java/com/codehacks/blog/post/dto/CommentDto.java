package com.codehacks.blog.post.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long id;

    @NotBlank(message = "Content is required")
    private String content;

    @NotBlank(message = "Author is required")
    private String author;

    @NotBlank
    private Long postId;

    private LocalDateTime createdAt;

}

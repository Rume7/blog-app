package com.codehacks.blog.post.mapper;

import com.codehacks.blog.post.dto.PostDTO;
import com.codehacks.blog.post.dto.PostSummaryDTO;
import com.codehacks.blog.post.model.Post;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public Post toEntity(PostDTO dto) {
        return new Post(dto.getTitle(), dto.getContent(), dto.getAuthor());
    }

    public PostDTO toDto(Post post) {
        return new PostDTO(post.getTitle(), post.getContent(), post.getAuthor());
    }

    public PostSummaryDTO toSummary(Post post) {
        return new PostSummaryDTO(post.getId(), post.getTitle(), post.getCreatedAt());
    }
}

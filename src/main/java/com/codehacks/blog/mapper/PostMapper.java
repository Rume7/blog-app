package com.codehacks.blog.mapper;

import com.codehacks.blog.dto.PostDTO;
import com.codehacks.blog.model.Post;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public Post toEntity(PostDTO dto) {
        return new Post(dto.getTitle(), dto.getContent(), dto.getAuthor());
    }

    public PostDTO toDto(Post post) {
        return new PostDTO(post.getTitle(), post.getContent(), post.getAuthor());
    }
}

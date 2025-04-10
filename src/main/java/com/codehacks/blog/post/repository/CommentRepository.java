package com.codehacks.blog.post.repository;

import com.codehacks.blog.post.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostId(Long postId);

    boolean existsByPostIdAndAuthorAndContent(Long postId, String author, String content);
}

package com.codehacks.blog.post.repository;

import com.codehacks.blog.post.model.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<PostComment, Long> {
}

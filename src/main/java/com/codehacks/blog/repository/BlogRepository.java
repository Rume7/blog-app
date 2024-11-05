package com.codehacks.blog.repository;

import com.codehacks.blog.model.Author;
import com.codehacks.blog.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Post, Long>{

    List<Post> findByAuthor(Author firstName);

    List<Post> findByTitleContainingIgnoreCase(String title);
}

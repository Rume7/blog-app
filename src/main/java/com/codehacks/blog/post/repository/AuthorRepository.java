package com.codehacks.blog.post.repository;

import com.codehacks.blog.post.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    Author findByEmail(String emailAddress);
}

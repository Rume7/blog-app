package com.codehacks.blog.service;

import com.codehacks.blog.model.Author;
import com.codehacks.blog.model.Post;
import com.codehacks.blog.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;

    public Author createAuthor(Author author) {
        Author newAuthor = new Author();
        newAuthor.setFirstName(author.getFirstName());
        newAuthor.setLastName(author.getLastName());
        List<Post> authorPosts = author.getPosts().stream().toList();
        newAuthor.setPosts(authorPosts);
        return authorRepository.save(newAuthor);
    }
}

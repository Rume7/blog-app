package com.codehacks.blog.controller;

import com.codehacks.blog.service.BlogService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@AllArgsConstructor
public class BlogController {

    private final BlogService blogService;


}

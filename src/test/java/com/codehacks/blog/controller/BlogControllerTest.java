package com.codehacks.blog.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BlogController.class)
class BlogControllerTests {

    private MockMvc mockMvc;

    @Test
    void testCreatePost() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content("{\"title\": \"Test Title\", \"content\": \"Test Content\"}"))
                .andExpect(status().isOk());
    }
}
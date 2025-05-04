package com.codehacks.blog.post.controller;

import com.codehacks.blog.config.TestConfig;
import com.codehacks.blog.post.dto.SubscriberDTO;
import com.codehacks.blog.post.model.Subscriber;
import com.codehacks.blog.post.model.SubscriptionStatus;
import com.codehacks.blog.post.service.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubscriptionController.class)
@Import(TestConfig.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionService subscriptionService;

    private SubscriberDTO subscriberDTO;
    private Subscriber subscriber;

    @BeforeEach
    void setUp() {
        subscriberDTO = new SubscriberDTO("test@example.com");
        subscriber = new Subscriber();
        subscriber.setEmail("test@example.com");
        subscriber.setStatus(SubscriptionStatus.ACTIVE);
        subscriber.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Given a valid email, when subscribing, then return 200 OK")
    void subscribe_ValidEmail_ShouldReturn200() throws Exception {
        // Given
        when(subscriptionService.subscribe(anyString())).thenReturn(subscriber);

        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subscriberDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given a valid email, when unsubscribing, then return 200 OK")
    void unsubscribe_ValidEmail_ShouldReturn200() throws Exception {
        // Given
        doNothing().when(subscriptionService).unsubscribe(anyString());

        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions/unsubscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subscriberDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given a valid email, when resubscribing, then return 200 OK")
    void resubscribe_ValidEmail_ShouldReturn200() throws Exception {
        // Given
        doNothing().when(subscriptionService).resubscribe(anyString());

        // When & Then
        mockMvc.perform(post("/api/v1/subscriptions/resubscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subscriberDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given authenticated user, when getting active subscribers, then return 200 OK")
    @WithMockUser(roles = "ADMIN")
    void getActiveSubscribers_ShouldReturn200() throws Exception {
        // Given
        List<Subscriber> subscribers = Arrays.asList(subscriber);
        when(subscriptionService.getActiveSubscribers()).thenReturn(subscribers);

        // When & Then
        mockMvc.perform(get("/api/v1/subscriptions/active"))
                .andExpect(status().isOk());
    }
} 
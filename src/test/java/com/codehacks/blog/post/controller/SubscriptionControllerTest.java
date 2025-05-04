package com.codehacks.blog.post.controller;

import com.codehacks.blog.auth.config.JwtAuthenticationFilter;
import com.codehacks.blog.post.dto.SubscriberDTO;
import com.codehacks.blog.post.exception.DuplicateSubscriptionException;
import com.codehacks.blog.post.exception.SubscriberNotFoundException;
import com.codehacks.blog.post.model.Subscriber;
import com.codehacks.blog.post.service.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SubscriptionController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = com.codehacks.blog.auth.exception.GlobalExceptionHandler.class)
})
@Import(com.codehacks.blog.post.exception.GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private SubscriptionService subscriptionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String email = "user@example.com";

    private final String BASE_URL = "/api/v1/subscription";

    @Test
    void subscribe_NewEmail_ShouldReturnCreated() throws Exception {
        // Given
        Subscriber subscriber = new Subscriber(email);
        when(subscriptionService.subscribe(email)).thenReturn(subscriber);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscriberDTO(email))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value("true"))
                .andExpect(jsonPath("$.data.email").value(email));
    }


    @Test
    void subscribe_Duplicate_ShouldReturnBadRequest() throws Exception {
        // Given
        when(subscriptionService.subscribe(email)).thenThrow(new DuplicateSubscriptionException("Already subscribed"));

        // When & Then
        mockMvc.perform(post(BASE_URL + "/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscriberDTO(email))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value("false"))
                .andExpect(jsonPath("$.message").value("Already subscribed"));
    }


    @Test
    void unsubscribe_Existing_ShouldReturnOk() throws Exception {
        // No need to mock as service returns void

        mockMvc.perform(post(BASE_URL + "/unsubscribe")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"))
                .andExpect(jsonPath("$.data").value("Successfully unsubscribed"));
    }


    @Test
    void unsubscribe_NonExistent_ShouldReturnNotFound() throws Exception {
        // Given
        doThrow(new SubscriberNotFoundException("Not found")).when(subscriptionService).unsubscribe(email);

        mockMvc.perform(post(BASE_URL + "/unsubscribe")
                        .param("email", email))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value("false"))
                .andExpect(jsonPath("$.message").value("Not found"));
    }


    @Test
    void resubscribe_Existing_ShouldReturnOk() throws Exception {
        // Given
        Subscriber subscriber = new Subscriber(email);
        when(subscriptionService.getActiveSubscribers())
                .thenReturn(List.of(subscriber));

        mockMvc.perform(post(BASE_URL + "/resubscribe")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"));
    }


    @DisplayName("Resubscribe non-existent subscriber - should return 404 Not Found")
    @Test
    void resubscribe_NonExistent_ShouldReturnNotFound() throws Exception {
        // Given
        String email = "notfound@example.com";

        when(subscriptionService.resubscribe(email))
                .thenThrow(new SubscriberNotFoundException("Subscriber with email " + email + " not found."));

        // When & Then
        mockMvc.perform(post(BASE_URL + "/resubscribe")
                        .param("email", email))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value("false"))
                .andExpect(jsonPath("$.message").value("Subscriber with email " + email + " not found."));

        verify(subscriptionService).resubscribe(email);
    }


    @Test
    void getActiveSubscribers_ShouldReturnList() throws Exception {
        // Given
        Subscriber subscriber = new Subscriber(email);
        when(subscriptionService.getActiveSubscribers()).thenReturn(List.of(subscriber));

        mockMvc.perform(get(BASE_URL + "/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"))
                .andExpect(jsonPath("$.data[0].email").value(email));
    }

    @Test
    void unsubscribe_InvalidEmail_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post(BASE_URL + "/unsubscribe")
                        .param("email", "invalid-email"))
                .andExpect(status().isBadRequest());
    }
}
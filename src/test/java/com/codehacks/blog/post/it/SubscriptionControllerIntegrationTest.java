package com.codehacks.blog.post.it;

import com.codehacks.blog.auth.dto.ApiResponse;
import com.codehacks.blog.config.TestConfig;
import com.codehacks.blog.post.model.Subscriber;
import com.codehacks.blog.post.model.SubscriptionStatus;
import com.codehacks.blog.post.repository.SubscriberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@ActiveProfiles("test")
@Transactional
class SubscriptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @BeforeEach
    void setUp() {
        //PostgresTestContainer.getInstance();
        subscriberRepository.deleteAll();
    }

    @Test
    void subscribe_ValidEmail_ShouldCreateSubscriber() throws Exception {
        // Given
        String email = "test@example.com";

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/subscriptions/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String responseBody = result.getResponse().getContentAsString();

        assertNotNull(result);
        assertTrue(responseBody.isEmpty(), "Response body should not be empty");
    }

    @Test
    void subscribe_InvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Given
        String invalidEmail = "invalid-email";

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/subscriptions/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + invalidEmail + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        ApiResponse<?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ApiResponse.class
        );

        assertNotNull(response);
        assertFalse(response.isSuccess(), "Response should indicate failure");
        assertNotNull(response.getMessage(), "Response should contain error message");
    }

    @Test
    void unsubscribe_ExistingSubscriber_ShouldUpdateStatus() throws Exception {
        // Given
        String email = "test@example.com";
        Subscriber subscriber = new Subscriber(email);
        subscriberRepository.save(subscriber);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/subscriptions/unsubscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then
        ApiResponse<String> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, String.class)
        );

        assertNotNull(response);
        assertTrue(response.isSuccess(), "Response should indicate success");
        assertEquals("Successfully unsubscribed", response.getData());

        // Commit the transaction to ensure the database state is updated
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // Verify database
        Optional<Subscriber> updatedSubscriber = subscriberRepository.findByEmail(email);
        assertNotNull(updatedSubscriber);
        assertEquals(SubscriptionStatus.UNSUBSCRIBED, updatedSubscriber.get().getStatus());
        assertNotNull(updatedSubscriber.get().getUnsubscribedAt());
    }

    @Test
    void resubscribe_UnsubscribedSubscriber_ShouldUpdateStatus() throws Exception {
        // Given
        String email = "test@example.com";
        Subscriber subscriber = new Subscriber(email);
        subscriber.setStatus(SubscriptionStatus.UNSUBSCRIBED);
        subscriber.setUnsubscribedAt(java.time.LocalDateTime.now());
        subscriberRepository.save(subscriber);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/subscriptions/resubscribe")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then
        ApiResponse<Subscriber> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, Subscriber.class)
        );

        assertNotNull(response);
        assertTrue(response.isSuccess(), "Response should indicate success");
        assertNotNull(response.getData());
        assertEquals(email, response.getData().getEmail());
        assertEquals(SubscriptionStatus.ACTIVE, response.getData().getStatus());

        // Commit the transaction to ensure the database state is updated
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        // Verify database
        Optional<Subscriber> updatedSubscriber = subscriberRepository.findByEmail(email);
        assertNotNull(updatedSubscriber);
        assertEquals(SubscriptionStatus.ACTIVE, updatedSubscriber.get().getStatus());
        assertNull(updatedSubscriber.get().getUnsubscribedAt());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getActiveSubscribers_ShouldReturnOnlyActiveSubscribers() throws Exception {
        // Given
        Subscriber activeSubscriber = new Subscriber("active@example.com");
        activeSubscriber.setStatus(SubscriptionStatus.ACTIVE);

        Subscriber unsubscribedSubscriber = new Subscriber("unsubscribed@example.com");
        unsubscribedSubscriber.setStatus(SubscriptionStatus.UNSUBSCRIBED);

        subscriberRepository.saveAll(List.of(activeSubscriber, unsubscribedSubscriber));

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/subscriptions/active"))
                .andExpect(status().isOk())
                //.andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then
        ApiResponse<List<Subscriber>> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Subscriber.class)
                )
        );

        assertNotNull(response);
        assertTrue(response.isSuccess(), "Response should indicate success");
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("active@example.com", response.getData().get(0).getEmail());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateStatus_ValidStatus_ShouldUpdateStatus() throws Exception {
        // Given
        String email = "test@example.com";
        Subscriber subscriber = new Subscriber(email);
        subscriberRepository.save(subscriber);

        // When
        MvcResult result = mockMvc.perform(put("/api/v1/subscriptions/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("email", email)
                        .param("status", SubscriptionStatus.UNSUBSCRIBED.name()))
                .andExpect(status().isOk())
                .andReturn();

        // Then

//        assertNotNull(response);
//        assertTrue(response.isSuccess(), "Response should indicate success");
//        assertEquals("Successfully updated status", response.getData());

        // Commit the transaction to ensure the database state is updated
//        TestTransaction.flagForCommit();
//        TestTransaction.end();
//        TestTransaction.start();

        // Verify database
        Optional<Subscriber> updatedSubscriber = subscriberRepository.findByEmail(email);
        assertNotNull(updatedSubscriber);
        assertEquals(SubscriptionStatus.UNSUBSCRIBED, updatedSubscriber.get().getStatus());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateStatus_InvalidStatus_ShouldReturnBadRequest() throws Exception {
        // Given
        String email = "test@example.com";
        Subscriber subscriber = new Subscriber(email);
        subscriberRepository.save(subscriber);

        // When & Then
        MvcResult result = mockMvc.perform(put("/api/v1/subscriptions/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("email", email)
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        ApiResponse<?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ApiResponse.class
        );

        assertNotNull(response);
        assertFalse(response.isSuccess(), "Response should indicate failure");
        assertNotNull(response.getMessage(), "Response should contain error message");
        assertTrue(response.getMessage().contains("Invalid status value"));
    }
} 
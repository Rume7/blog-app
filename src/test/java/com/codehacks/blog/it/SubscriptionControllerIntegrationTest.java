package com.codehacks.blog.it;

import com.codehacks.blog.auth.config.JwtAuthenticationFilter;
import com.codehacks.blog.subscription.dto.SubscriberDTO;
import com.codehacks.blog.subscription.model.Subscriber;
import com.codehacks.blog.subscription.model.SubscriptionStatus;
import com.codehacks.blog.subscription.repository.SubscriberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class SubscriptionControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8-alpine")
            .withDatabaseName("blog_test_db")
            .withUsername("testUser")
            .withPassword("testPass")
            .withReuse(false);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Bucket rateLimiterBucket;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private SubscriberRepository subscriberRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String email = "user@example.com";

    @BeforeEach
    void setup() {
        given(rateLimiterBucket.tryConsume(anyLong())).willReturn(true);
        //subscriberRepository.deleteAll();
    }

    @Test
    @DisplayName("Subscribe new email - should return 201 Created")
    void subscribe_New_ShouldReturnCreated() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions/subscribe")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscriberDTO(email))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email));

        List<Subscriber> subscribers = subscriberRepository.findAll();
        assertThat(subscribers).hasSize(1);
    }

    @Test
    @DisplayName("Subscribe duplicate email - should return 409 Conflict")
    void subscribe_Duplicate_ShouldReturnConflict() throws Exception {
        // Given existing subscriber
        subscriberRepository.save(new Subscriber(email));

        mockMvc.perform(post("/api/v1/subscriptions/subscribe")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscriberDTO(email))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already subscribed: " + email));
    }

    @Test
    @DisplayName("Unsubscribe existing subscriber - should return 200 OK")
    void unsubscribe_Existing_ShouldReturnOk() throws Exception {
        subscriberRepository.save(new Subscriber(email));

        mockMvc.perform(post("/api/v1/subscriptions/unsubscribe")
                        .param("email", email))
                .andExpect(status().isOk())
                //.andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Successfully unsubscribed"));

        Subscriber updated = subscriberRepository.findByEmail(email).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.UNSUBSCRIBED);
    }

    @Test
    @DisplayName("Unsubscribe non-existent subscriber - should return 404 Not Found")
    void unsubscribe_NonExistent_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions/unsubscribe")
                        .param("email", "notfound@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Subscriber with email notfound@example.com not found."));
    }

    @Test
    @DisplayName("Resubscribe existing unsubscribed subscriber - should return 200 OK")
    void resubscribe_Existing_ShouldReturnOk() throws Exception {
        Subscriber sub = new Subscriber(email);
        sub.setStatus(SubscriptionStatus.UNSUBSCRIBED);
        subscriberRepository.save(sub);

        mockMvc.perform(post("/api/v1/subscriptions/resubscribe")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email));

        Subscriber updated = subscriberRepository.findByEmail(email).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Resubscribe non-existent subscriber - should return 404 Not Found")
    void resubscribe_NonExistent_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions/resubscribe")
                        .param("email", "notfound@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Subscriber with email notfound@example.com not found."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Get active subscribers - should return list")
    void getActiveSubscribers_ShouldReturnList() throws Exception {
        subscriberRepository.save(new Subscriber(email));

        mockMvc.perform(get("/api/v1/subscriptions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value(email));
    }
}

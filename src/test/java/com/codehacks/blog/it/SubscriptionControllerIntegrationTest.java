package com.codehacks.blog.it;

import com.codehacks.blog.subscription.dto.SubscriberDTO;
import com.codehacks.blog.subscription.model.Subscriber;
import com.codehacks.blog.subscription.model.SubscriptionStatus;
import com.codehacks.blog.subscription.service.SubscriptionServiceImpl;
import com.codehacks.blog.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
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

    @Autowired
    private SubscriptionServiceImpl subscriptionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String email = "user@example.com";

    @BeforeEach
    void setup() {
        given(rateLimiterBucket.tryConsume(anyLong())).willReturn(true);
        subscriptionService.deleteAllSubscribers();
    }

    @Test
    @DisplayName("Subscribe new email - should return 201 Created")
    void subscribe_New_ShouldReturnCreated() throws Exception {
        mockMvc.perform(post(Constants.SUBSCRIPTION_PATH + "/subscribe")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscriberDTO(email))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email));

        List<Subscriber> subscribers = subscriptionService.getAllSubscribers();
        assertThat(subscribers).hasSize(1);
    }

    @Test
    @DisplayName("Subscribe duplicate email - should return 409 Conflict")
    void subscribe_Duplicate_ShouldReturnConflict() throws Exception {
        // Given existing subscriber
        subscriptionService.subscribe(email);

        mockMvc.perform(post(Constants.SUBSCRIPTION_PATH + "/subscribe")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscriberDTO(email))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already subscribed: " + email));
    }

    @Test
    @DisplayName("Unsubscribe existing subscriber - should return 200 OK")
    void unsubscribe_Existing_ShouldReturnOk() throws Exception {
        subscriptionService.subscribe(email);

        mockMvc.perform(post(Constants.SUBSCRIPTION_PATH + "/unsubscribe")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Successfully unsubscribed"));

        Subscriber updated = subscriptionService.findSubscriberByEmail(email).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.UNSUBSCRIBED);
    }

    @Test
    @DisplayName("Unsubscribe non-existent subscriber - should return 404 Not Found")
    void unsubscribe_NonExistent_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(post(Constants.SUBSCRIPTION_PATH + "/unsubscribe")
                        .param("email", "notfound@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Subscriber with email notfound@example.com not found."));
    }

    @Test
    @DisplayName("Resubscribe existing unsubscribed subscriber - should return 200 OK")
    void resubscribe_Existing_ShouldReturnOk() throws Exception {
        subscriptionService.subscribe(email);
        subscriptionService.unsubscribe(email);

        mockMvc.perform(post(Constants.SUBSCRIPTION_PATH + "/resubscribe")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email));

        Subscriber updated = subscriptionService.findSubscriberByEmail(email).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Resubscribe non-existent subscriber - should return 404 Not Found")
    void resubscribe_NonExistent_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(post(Constants.SUBSCRIPTION_PATH + "/resubscribe")
                        .param("email", "notfound@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Subscriber with email notfound@example.com not found."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Get active subscribers - should return list")
    void getActiveSubscribers_ShouldReturnList() throws Exception {
        subscriptionService.subscribe(email);

        mockMvc.perform(get(Constants.SUBSCRIPTION_PATH + "/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value(email));
    }

    private void initializeSubscribers() {
        Subscriber sub1 = new Subscriber("active1@example.com");
        sub1.setStatus(SubscriptionStatus.ACTIVE);
        Subscriber sub2 = new Subscriber("inactive1@example.com");
        sub2.setStatus(SubscriptionStatus.UNSUBSCRIBED);

        Subscriber sub3 = new Subscriber("active2@example.com");
        sub3.setStatus(SubscriptionStatus.ACTIVE);
        Subscriber sub4 = new Subscriber("pending1@example.com");
        sub4.setStatus(SubscriptionStatus.BANNED);

        subscriptionService.saveSubscribersList(List.of(sub1, sub2, sub3, sub4));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Get subscribers by their status- should return a Map")
    void shouldReturnGroupedSubscribers_ForAdmin() throws Exception {
        initializeSubscribers();

        mockMvc.perform(get(Constants.SUBSCRIPTION_PATH + "/grouped-by-status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ACTIVE", hasSize(2)))
                .andExpect(jsonPath("$.data.UNSUBSCRIBED", hasSize(1)))
                .andExpect(jsonPath("$.data.BANNED", hasSize(1)))
                .andExpect(jsonPath("$.data.ACTIVE[0].email").value("active1@example.com"))
                .andExpect(jsonPath("$.data.ACTIVE[1].email").value("active2@example.com"))
                .andExpect(jsonPath("$.data.UNSUBSCRIBED[0].email").value("inactive1@example.com"))
                .andExpect(jsonPath("$.data.BANNED[0].email").value("pending1@example.com"));
    }
}

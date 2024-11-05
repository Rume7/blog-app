package com.codehacks.blog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimiterFilterTest {

    private RateLimiterFilter rateLimitFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimiterFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void testSuccessfulRequest() throws ServletException, IOException {
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void testRateLimitExceeded() throws ServletException, IOException {
        // Create new response for each request to avoid content accumulation
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate rapid requests until rate limit is exceeded
        while (response.getStatus() != HttpStatus.TOO_MANY_REQUESTS.value()) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
        assertEquals("Too many requests", response.getContentAsString());
    }

    @Test
    void testRateLimitRecovery() throws ServletException, IOException, InterruptedException {
        // First request - should succeed
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        assertEquals(HttpStatus.OK.value(), response.getStatus());

        // Wait for rate limit to reset
        Thread.sleep(1000);

        // New request after waiting - should succeed
        MockHttpServletResponse newResponse = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, newResponse, filterChain);
        assertEquals(HttpStatus.OK.value(), newResponse.getStatus());
    }

    @Test
    void testConcurrentRequests() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    MockHttpServletResponse threadResponse = new MockHttpServletResponse();
                    rateLimitFilter.doFilterInternal(request, threadResponse, filterChain);
                    if (threadResponse.getStatus() == HttpStatus.OK.value()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Handle exception
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);
        assertTrue(successCount.get() > 0 && successCount.get() < threadCount);
    }

    @Test
    void testDifferentEndpoints() throws ServletException, IOException, InterruptedException {
        // Test first endpoint
        request.setRequestURI("/api/posts");
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        assertEquals(HttpStatus.OK.value(), response.getStatus());

        // Wait for rate limit to reset
        Thread.sleep(1000);

        // Test second endpoint
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        request.setRequestURI("/api/users");
        rateLimitFilter.doFilterInternal(request, response2, filterChain);
        assertEquals(HttpStatus.OK.value(), response2.getStatus());
    }

    @Test
    void testDifferentHttpMethods() throws ServletException, IOException, InterruptedException {
        // Test rate limiting for different HTTP methods
        request.setMethod("GET");
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        assertEquals(HttpStatus.OK.value(), response.getStatus());

        Thread.sleep(1000);

        MockHttpServletResponse response2 = new MockHttpServletResponse();
        request.setMethod("POST");
        rateLimitFilter.doFilterInternal(request, response2, filterChain);
        assertEquals(HttpStatus.OK.value(), response2.getStatus());
    }

    @Test
    void testBurstRequests() throws ServletException, IOException {
        List<MockHttpServletResponse> responses = new ArrayList<>();

        // Send burst of requests
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse burstResponse = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, burstResponse, filterChain);
            responses.add(burstResponse);
        }

        long successfulRequests = responses.stream()
                .filter(r -> r.getStatus() == HttpStatus.OK.value())
                .count();
        assertTrue(successfulRequests > 0);
    }

    @Test
    void testDifferentIpAddresses() throws ServletException, IOException, InterruptedException {
        // Test request from first IP
        request.setRemoteAddr("192.168.1.1");
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        assertEquals(HttpStatus.OK.value(), response.getStatus());

        Thread.sleep(1000);

        // Test request from second IP
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        request.setRemoteAddr("192.168.1.2");
        rateLimitFilter.doFilterInternal(request, response2, filterChain);
        assertEquals(HttpStatus.OK.value(), response2.getStatus());
    }

    @Test
    void testSameIpMultipleRequests() throws ServletException, IOException {
        request.setRemoteAddr("192.168.1.100");

        // First request should succeed
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        assertEquals(HttpStatus.OK.value(), response.getStatus());

        // Rapid subsequent request should be rate limited
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(request, response2, filterChain);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response2.getStatus());
    }

    @Test
    void testIpv6Addresses() throws ServletException, IOException, InterruptedException {
        // Test IPv6 addresses
        request.setRemoteAddr("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        assertEquals(HttpStatus.OK.value(), response.getStatus());

        Thread.sleep(1000);

        MockHttpServletResponse response2 = new MockHttpServletResponse();
        request.setRemoteAddr("2001:0db8:85a3:0000:0000:8a2e:0370:7335");
        rateLimitFilter.doFilterInternal(request, response2, filterChain);
        assertEquals(HttpStatus.OK.value(), response2.getStatus());
    }

    @Test
    void testLocalHostRequests() throws ServletException, IOException {
        request.setRemoteAddr("127.0.0.1");
        rateLimitFilter.doFilterInternal(request, response, filterChain);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

}

package com.codehacks.blog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter writer;

    private static final String TEST_IP = "192.168.1.1";
    private static final int MAX_REQUESTS = 5;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    @BeforeEach
    void setUp() {
        // Given
        ReflectionTestUtils.setField(rateLimitingFilter, "maxRequests", MAX_REQUESTS);
        ReflectionTestUtils.setField(rateLimitingFilter, "timeWindow", 1);

        // When
        when(request.getRemoteAddr()).thenReturn(TEST_IP);

        // Reset the requestCounts map before each test
        ReflectionTestUtils.setField(rateLimitingFilter, "requestCounts", new ConcurrentHashMap<>());
    }

    private void simulateRequests(int count) throws IOException, ServletException {
        for (int i = 0; i < count; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }
    }

    @Test
    void givenUnderLimitRequestsWhenDoFilterProceedToFilterChain() throws IOException, ServletException {
        // Given & When
        simulateRequests(4);

        // Then
        verify(filterChain, times(4)).doFilter(request, response);
        verify(response, times(4)).setHeader(eq("X-RateLimit-Limit"), eq("5"));
        verify(response, times(4)).setHeader(eq("X-RateLimit-Remaining"), anyString());
        verify(response, times(4)).setHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    void givenOverLimitRequestsWhenDoFilterReturnTooManyRequests() throws IOException, ServletException {
        // Given & When
        when(response.getWriter()).thenReturn(writer);
        simulateRequests(MAX_REQUESTS);

        // Simulate one more request, which should trigger the rate limit
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(HTTP_TOO_MANY_REQUESTS);
        verify(writer).write("Too many requests. Please try again later");
        verify(filterChain, times(MAX_REQUESTS)).doFilter(request, response);
    }

    @Test
    void givenTimeWindowHasPassedWhenDoFilterAllowRequestsAgain() throws IOException, ServletException {
        // Given
        Clock initialClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        rateLimitingFilter.setClock(initialClock);

        simulateRequests(MAX_REQUESTS);

        Clock laterClock = Clock.offset(initialClock, Duration.ofSeconds(2));
        rateLimitingFilter.setClock(laterClock);

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain, times(MAX_REQUESTS + 1)).doFilter(request, response);
        verify(response, times(6)).setHeader(eq("X-RateLimit-Limit"), eq("5"));
    }

    @Test
    void givenDifferentIpAddressesWhenDoFilterTrackSeparately() throws IOException, ServletException {
        // Given & When: First IP
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        simulateRequests(MAX_REQUESTS);

        // Given & When: Second IP
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        simulateRequests(MAX_REQUESTS);

        // Then
        verify(filterChain, times(10)).doFilter(request, response);
    }

    @Test
    void whenFilterChainThrowsExceptionPropagateException() throws IOException, ServletException {
        // Given
        ServletException testException = new ServletException("Test exception");

        // When
        doThrow(testException).when(filterChain).doFilter(request, response);

        // Then
        assertThrows(ServletException.class, () ->
                rateLimitingFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void givenOverLimitRequestsWhenDoFilterSetRateLimitHeaders() throws IOException, ServletException {
        // Given & When
        when(response.getWriter()).thenReturn(writer);
        final int MORE_REQUESTS = MAX_REQUESTS + 1;
        simulateRequests(MORE_REQUESTS);

        // Then
        verify(response, times(MORE_REQUESTS)).setHeader(eq("X-RateLimit-Limit"), eq("5"));
        verify(response, atLeastOnce()).setHeader(eq("X-RateLimit-Remaining"), eq("0"));
        verify(response, times(MORE_REQUESTS)).setHeader(eq("X-RateLimit-Reset"), anyString());
    }
}

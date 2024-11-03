package com.codehacks.blog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private Clock clock = Clock.systemDefaultZone();

    @Value("${rate.limit.requests:5}")
    private int maxRequests;

    @Value("${rate.limit.time-window:1}")
    private int timeWindow;

    private static final int SC_TOO_MANY_REQUESTS = 429;

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String clientIP = request.getRemoteAddr();
        RequestCounter counter = requestCounts.computeIfAbsent(clientIP, k -> new RequestCounter(clock.millis()));

        if (!counter.isRequestAllowed(response)) {
            response.setStatus(SC_TOO_MANY_REQUESTS);
            response.getWriter().write("Too many requests. Please try again later");
            return;
        }

        filterChain.doFilter(request, response);
    }

    protected void setClock(Clock fixedClock) {
        this.clock = fixedClock;
    }

    private class RequestCounter {
        private int requestCount = 0;
        private long startTime;

        public RequestCounter(long startTime) {
            this.startTime = startTime;
        }

        public synchronized boolean isRequestAllowed(HttpServletResponse response) {
            long currentTime = clock.millis();

            if (currentTime - startTime > TimeUnit.SECONDS.toMillis(timeWindow)) {
                requestCount = 0;
                startTime = currentTime;
            }

            requestCount++;
            
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequests - requestCount)));
            response.setHeader("X-RateLimit-Reset", String.valueOf(startTime + TimeUnit.SECONDS.toMillis(timeWindow)));
            
            return requestCount <= maxRequests;
        }
    }
}

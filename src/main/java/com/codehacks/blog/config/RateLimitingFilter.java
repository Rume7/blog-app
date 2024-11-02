package com.codehacks.blog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${rate.limit.requests:5}")
    private int maxRequests;

    @Value("${rate.limit.time-window:1}")
    private int timeWindow;

    private static final int SC_TOO_MANY_REQUESTS = 429;

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String clientIP = request.getRemoteAddr();
        RequestCounter counter = requestCounts.computeIfAbsent(clientIP, k -> new RequestCounter());

        if (!counter.isRequestAllowed()) {
            response.setStatus(SC_TOO_MANY_REQUESTS);
            response.getWriter().write("Too many requests. Please try again later");
            return;
        }

        filterChain.doFilter(request, response);
    }


    private class RequestCounter {

        private int requestCount = 0;
        private long startTime = System.currentTimeMillis();

        public synchronized boolean isRequestAllowed() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - startTime > TimeUnit.SECONDS.toMillis(timeWindow)) {
                // Reset the counter after the time window has expired
                requestCount = 0;
                startTime = currentTime;
            }
            requestCount++;
            return requestCount <= maxRequests;
        }
    }
}

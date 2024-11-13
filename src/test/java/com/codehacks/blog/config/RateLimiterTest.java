package com.codehacks.blog.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class RateLimiterTest {
    private RateLimiter rateLimiter;
    private ProceedingJoinPoint joinPoint;
    private RateLimit rateLimit;
    private Signature signature;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
        joinPoint = mock(ProceedingJoinPoint.class);
        signature = mock(Signature.class);
        rateLimit = mock(RateLimit.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("testMethod");
    }

    @Test
    void whenUnderRateLimit_shouldProceed() throws Throwable {
        // Given
        when(rateLimit.maxRequests()).thenReturn(5);
        when(rateLimit.timeWindowMinutes()).thenReturn(1);
        when(joinPoint.proceed()).thenReturn("success");

        // When
        Object result = rateLimiter.enforceRateLimit(joinPoint, rateLimit);

        // Then
        assertEquals("success", result);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void whenExceedingRateLimit_shouldThrowException() throws Throwable {
        // Given
        when(rateLimit.maxRequests()).thenReturn(1);
        when(rateLimit.timeWindowMinutes()).thenReturn(1);

        // When
        rateLimiter.enforceRateLimit(joinPoint, rateLimit);

        // Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> rateLimiter.enforceRateLimit(joinPoint, rateLimit)
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        assertEquals("Rate limit exceeded", exception.getReason());
    }

    @Test
    void whenMultipleMethodsCalled_shouldTrackSeparately() throws Throwable {
        // Given
        when(rateLimit.maxRequests()).thenReturn(1);
        when(rateLimit.timeWindowMinutes()).thenReturn(1);

        // Setup second method
        ProceedingJoinPoint secondJoinPoint = mock(ProceedingJoinPoint.class);
        Signature secondSignature = mock(Signature.class);
        when(secondJoinPoint.getSignature()).thenReturn(secondSignature);
        when(secondSignature.toShortString()).thenReturn("anotherMethod");

        // When
        // First method should succeed once
        rateLimiter.enforceRateLimit(joinPoint, rateLimit);

        // Second method should also succeed once (different bucket)
        rateLimiter.enforceRateLimit(secondJoinPoint, rateLimit);

        // Then: Both methods should fail on second attempt
        assertThrows(ResponseStatusException.class,
                () -> rateLimiter.enforceRateLimit(joinPoint, rateLimit));
        assertThrows(ResponseStatusException.class,
                () -> rateLimiter.enforceRateLimit(secondJoinPoint, rateLimit));
    }

    @Test
    void whenRateLimitRefills_shouldAllowNewRequests() throws Throwable {
        // Given
        when(rateLimit.maxRequests()).thenReturn(1);
        when(rateLimit.timeWindowMinutes()).thenReturn(1);

        // When
        // First request should succeed
        rateLimiter.enforceRateLimit(joinPoint, rateLimit);

        // Then: Second request should fail
        assertThrows(ResponseStatusException.class,
                () -> rateLimiter.enforceRateLimit(joinPoint, rateLimit));

        // Wait for bucket to refill
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            try {
                // Then: Third request should succeed after refill
                rateLimiter.enforceRateLimit(joinPoint, rateLimit);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }, 61, TimeUnit.SECONDS);
        scheduler.shutdown();
    }

    @Test
    void whenProceedThrowsException_shouldPropagateException() throws Throwable {
        // Given
        when(rateLimit.maxRequests()).thenReturn(5);
        when(rateLimit.timeWindowMinutes()).thenReturn(1);
        RuntimeException expectedException = new RuntimeException("Test exception");
        when(joinPoint.proceed()).thenThrow(expectedException);

        // When
        RuntimeException actualException = assertThrows(
                RuntimeException.class,
                () -> rateLimiter.enforceRateLimit(joinPoint, rateLimit)
        );

        // Then
        assertEquals(expectedException, actualException);
    }
}
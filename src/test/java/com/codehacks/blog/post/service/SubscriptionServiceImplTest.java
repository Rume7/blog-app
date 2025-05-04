package com.codehacks.blog.post.service;

import com.codehacks.blog.post.exception.DuplicateSubscriptionException;
import com.codehacks.blog.post.exception.SubscriberNotFoundException;
import com.codehacks.blog.post.model.Subscriber;
import com.codehacks.blog.post.model.SubscriptionStatus;
import com.codehacks.blog.post.repository.SubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriberRepository subscriberRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private Subscriber activeSubscriber;
    private Subscriber unsubscribedSubscriber;

    @BeforeEach
    void setUp() {
        activeSubscriber = new Subscriber("test@example.com");
        activeSubscriber.setStatus(SubscriptionStatus.ACTIVE);
        activeSubscriber.setCreatedAt(LocalDateTime.now());

        unsubscribedSubscriber = new Subscriber("unsubscribed@example.com");
        unsubscribedSubscriber.setStatus(SubscriptionStatus.UNSUBSCRIBED);
        unsubscribedSubscriber.setCreatedAt(LocalDateTime.now());
        unsubscribedSubscriber.setUnsubscribedAt(LocalDateTime.now());
    }

    @Test
    void subscribe_NewEmail_ShouldCreateNewSubscriber() {
        // Given
        String email = "new@example.com";

        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(invocation -> {
            Subscriber subscriber = invocation.getArgument(0);
            subscriber.setCreatedAt(LocalDateTime.now());
            return subscriber;
        });

        // When
        Subscriber result = subscriptionService.subscribe(email);

        // Then
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNull(result.getUnsubscribedAt());

        verify(subscriberRepository).save(any(Subscriber.class));
    }

    @Test
    void subscribe_ExistingActive_ShouldThrowDuplicateSubscriptionException() {
        // Given
        String email = "new@example.com";

        Subscriber existing = new Subscriber(email);
        existing.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.of(existing));

        // Expect
        assertThrows(DuplicateSubscriptionException.class, () -> subscriptionService.subscribe(email));
        verify(subscriberRepository, never()).save(any());
    }


    @Test
    void subscribe_ExistingInactiveSubscriber_ShouldResubscribe() {
        // Given
        String email = "unsubscribed@example.com";

        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.ofNullable(unsubscribedSubscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Subscriber result = subscriptionService.subscribe(email);

        // Then
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getUnsubscribedAt());
        verify(subscriberRepository).save(any(Subscriber.class));
    }

    @Test
    void unsubscribe_ExistingSubscriber_ShouldUpdateStatus() {
        // Given
        String email = "existing@example.com";
        Subscriber existingSubscriber = new Subscriber(email);
        existingSubscriber.setStatus(SubscriptionStatus.ACTIVE);

        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.of(existingSubscriber));

        // When
        subscriptionService.unsubscribe(email);

        // Then
        assertEquals(SubscriptionStatus.UNSUBSCRIBED, existingSubscriber.getStatus());
        assertNotNull(existingSubscriber.getUnsubscribedAt());

        verify(subscriberRepository).save(existingSubscriber);
    }


    @Test
    void unsubscribe_NonExistentSubscriber_ShouldDoNothing() {
        // Given
        String email = "nonexistent@example.com";
        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(SubscriberNotFoundException.class, () -> subscriptionService.unsubscribe(email));

        // Then
        verify(subscriberRepository, never()).save(any(Subscriber.class));
    }

    @Test
    void resubscribe_ExistingUnsubscribedSubscriber_ShouldUpdateStatus() {
        // Given
        String email = "unsubscribed@example.com";
        unsubscribedSubscriber = new Subscriber(email);
        unsubscribedSubscriber.setStatus(SubscriptionStatus.UNSUBSCRIBED);

        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.of(unsubscribedSubscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(i -> i.getArgument(0));

        // When
        subscriptionService.resubscribe(email);

        // Then
        assertEquals(SubscriptionStatus.ACTIVE, unsubscribedSubscriber.getStatus());
        verify(subscriberRepository, times(1)).findByEmail(email);
        verify(subscriberRepository, times(1)).save(unsubscribedSubscriber);
    }

    @Test
    void resubscribe_NonExistent_ShouldThrow() {
        // Given
        String email = "non-existent/@example.com";

        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Expect
        assertThrows(SubscriberNotFoundException.class, () -> subscriptionService.resubscribe(email));
        verify(subscriberRepository, never()).save(any());
    }

    @Test
    void getActiveSubscribers_ShouldReturnOnlyActiveSubscribers() {
        // Given
        List<Subscriber> allSubscribers = Arrays.asList(activeSubscriber, unsubscribedSubscriber);
        when(subscriberRepository.findByStatus(SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(activeSubscriber));

        // When
        List<Subscriber> result = subscriptionService.getActiveSubscribers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(SubscriptionStatus.ACTIVE, result.get(0).getStatus());
    }
} 
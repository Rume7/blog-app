package com.codehacks.blog.post.service;

import com.codehacks.blog.post.exception.SubscriberNotFoundException;
import com.codehacks.blog.post.model.Subscriber;
import com.codehacks.blog.post.model.SubscriptionStatus;
import com.codehacks.blog.post.repository.SubscriberRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriberRepository subscriberRepository;

    @Override
    @Transactional
    public Subscriber subscribe(String email) {
        Optional<Subscriber> subscriber = subscriberRepository.findByEmail(email);
        if (subscriber.isPresent()) {
            Subscriber existingSubscriber = subscriber.get();
            if (existingSubscriber.getStatus() != SubscriptionStatus.ACTIVE) {
                updateStatus(existingSubscriber.getEmail(), SubscriptionStatus.ACTIVE);
                return existingSubscriber;
            }
            return null;
        }
        Subscriber newSubscriber = new Subscriber(email);
        return subscriberRepository.save(newSubscriber);
    }

    @Override
    public void unsubscribe(String email) {
        Subscriber subscriber = subscriberRepository.findByEmail(email)
                .orElseThrow(() -> new SubscriberNotFoundException("Subscriber with email " + email + " not found."));

        updateStatus(subscriber, SubscriptionStatus.UNSUBSCRIBED);
    }

    @Override
    public void resubscribe(String email) {
        Optional<Subscriber> subscriber = subscriberRepository.findByEmail(email);
        subscriber.ifPresent(value -> updateStatus(subscriber.get(), SubscriptionStatus.ACTIVE));
    }

    @Override
    public List<Subscriber> getActiveSubscribers() {
        return subscriberRepository.findByStatus(SubscriptionStatus.ACTIVE);
    }

    @Transactional
    private void updateStatus(String email, SubscriptionStatus status) {
        Optional<Subscriber> subscriber = subscriberRepository.findByEmail(email);
        if (subscriber.isPresent()) {
            Subscriber foundSubscriber = subscriber.get();
            foundSubscriber.setStatus(status);
            foundSubscriber.setUnsubscribedAt(LocalDateTime.now());
            subscriberRepository.save(foundSubscriber);
            return;
        }
        throw new RuntimeException("Subscriber not found");
    }

    @Transactional
    private void updateStatus(Subscriber foundSubscriber, SubscriptionStatus status) {
        foundSubscriber.setStatus(status);
        foundSubscriber.setUnsubscribedAt(LocalDateTime.now());
        subscriberRepository.save(foundSubscriber);
    }
} 
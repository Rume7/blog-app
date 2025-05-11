package com.codehacks.blog.subscription.service;

import com.codehacks.blog.subscription.exception.DuplicateSubscriptionException;
import com.codehacks.blog.subscription.exception.SubscriberNotFoundException;
import com.codehacks.blog.subscription.model.Subscriber;
import com.codehacks.blog.subscription.model.SubscriptionStatus;
import com.codehacks.blog.subscription.repository.SubscriberRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriberRepository subscriberRepository;

    @Override
    @Transactional
    public Subscriber subscribe(String email) {
        return subscriberRepository.findByEmail(email)
                .map(existingSubscriber -> {
                    if (existingSubscriber.getStatus() == SubscriptionStatus.ACTIVE) {
                        throw new DuplicateSubscriptionException("Email is already subscribed: " + email);
                    }
                    updateStatus(existingSubscriber, SubscriptionStatus.ACTIVE);
                    return existingSubscriber;
                })
                .orElseGet(() -> subscriberRepository.save(new Subscriber(email)));
    }

    @Override
    @Transactional
    public void unsubscribe(String email) {
        Subscriber subscriber = subscriberRepository.findByEmail(email)
                .orElseThrow(() -> new SubscriberNotFoundException("Subscriber with email " + email + " not found."));
        updateStatus(subscriber, SubscriptionStatus.UNSUBSCRIBED);
    }

    @Override
    public Subscriber resubscribe(String email) {
        Subscriber subscriber = subscriberRepository.findByEmail(email)
                .orElseThrow(() -> new SubscriberNotFoundException("Subscriber with email " + email + " not found."));

        updateStatus(subscriber, SubscriptionStatus.ACTIVE);
        return subscriber;
    }

    @Override
    public List<Subscriber> getActiveSubscribers() {
        return subscriberRepository.findByStatus(SubscriptionStatus.ACTIVE);
    }

    private void updateStatus(Subscriber subscriber, SubscriptionStatus status) {
        subscriber.setStatus(status);
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        subscriberRepository.save(subscriber);
    }
}

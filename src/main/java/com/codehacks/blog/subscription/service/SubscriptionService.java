package com.codehacks.blog.subscription.service;

import com.codehacks.blog.subscription.model.Subscriber;
import com.codehacks.blog.subscription.model.SubscriptionStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SubscriptionService {

    Subscriber subscribe(String email);

    void unsubscribe(String email);

    Subscriber resubscribe(String email);

    List<Subscriber> getActiveSubscribers();

    Map<SubscriptionStatus, List<Subscriber>> getSubscribersByStatus();

    List<Subscriber> saveSubscribersList(List<Subscriber> subscriberList);

    List<Subscriber> getAllSubscribers();

    void deleteAllSubscribers();

    Optional<Subscriber> findSubscriberByEmail(String email);
} 
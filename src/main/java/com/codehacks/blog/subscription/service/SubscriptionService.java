package com.codehacks.blog.subscription.service;

import com.codehacks.blog.subscription.model.Subscriber;
import com.codehacks.blog.subscription.model.SubscriptionStatus;

import java.util.List;
import java.util.Map;

public interface SubscriptionService {

    Subscriber subscribe(String email);

    void unsubscribe(String email);

    Subscriber resubscribe(String email);

    List<Subscriber> getActiveSubscribers();

    Map<SubscriptionStatus, List<Subscriber>> getSubscribersByStatus();
} 
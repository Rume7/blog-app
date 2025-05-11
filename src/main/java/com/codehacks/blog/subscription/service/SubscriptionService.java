package com.codehacks.blog.subscription.service;

import com.codehacks.blog.subscription.model.Subscriber;

import java.util.List;

public interface SubscriptionService {

    Subscriber subscribe(String email);

    void unsubscribe(String email);

    Subscriber resubscribe(String email);

    List<Subscriber> getActiveSubscribers();
} 
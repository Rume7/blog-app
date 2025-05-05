package com.codehacks.blog.post.service;

import com.codehacks.blog.post.model.Subscriber;

import java.util.List;

public interface SubscriptionService {

    Subscriber subscribe(String email);

    void unsubscribe(String email);

    Subscriber resubscribe(String email);

    List<Subscriber> getActiveSubscribers();
} 
package com.codehacks.blog.subscription.repository;

import com.codehacks.blog.subscription.model.Subscriber;
import com.codehacks.blog.subscription.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    List<Subscriber> findByStatus(SubscriptionStatus status);

    Optional<Subscriber> findByEmail(String email);
} 
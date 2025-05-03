package com.codehacks.blog.post.repository;

import com.codehacks.blog.post.model.Subscriber;
import com.codehacks.blog.post.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    List<Subscriber> findByStatus(SubscriptionStatus status);

    Optional<Subscriber> findByEmail(String email);
} 